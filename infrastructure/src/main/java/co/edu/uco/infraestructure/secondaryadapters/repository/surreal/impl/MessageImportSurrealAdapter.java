package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageImportRepository;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.*;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.*;

@Repository
public class MessageImportSurrealAdapter extends SurrealCatalogSupport implements MessageImportRepository {
    private final ObjectMapper json;
    private final CatalogPort catalog;

    public MessageImportSurrealAdapter(Surreal surreal, LoggingPortFactory logs, ObjectMapper json, CatalogPort catalog) {
        super(surreal, logs.getLogger(MessageImportSurrealAdapter.class));
        this.json = json;
        this.catalog = catalog;
    }

    @Override
    public Optional<MessageImportReceipt> findReceipt(String importId, String applicationId) {
        try {
            var sql = "SELECT receipt_json FROM " + recordIdLiteral("message_import_receipt", importId)
                    + " WHERE application_id = " + recordIdLiteral("application", applicationId) + ";";
            var payload = queryOne(sql, catalog.getMessage(TCH_079.getCode()), row -> stringOf(row.get("receipt_json")));
            if (payload.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(json.readValue(payload.get(), MessageImportReceipt.class));
        } catch (JsonProcessingException | RuntimeException ex) {
            throw failure(ex);
        }
    }

    @Override
    public MessageImportReceipt createAtomically(MessageImportPreview preview, String environmentStateId) {
        var receipt = new MessageImportReceipt(preview.importId(), preview.rows().size(), preview.environmentIds(), Instant.now());
        var sql = transaction(preview, environmentStateId, receipt);
        try {
            var response = surreal.query(sql);
            // The native driver can defer a statement error until its result is consumed.
            for (int index = 0; index < response.size(); index++) {
                response.take(index);
            }
        } catch (RuntimeException ex) {
            var completed = findReceipt(preview.importId(), preview.applicationId());
            if (completed.isPresent()) {
                return completed.get();
            }
            log.error(catalog.getMessage(TCH_079.getCode()), ex);
            throw ConflictException.buildUserException(catalog.getMessage(IMP_010.getCode()));
        }
        // A receipt in the same transaction proves the entire batch was committed.
        return findReceipt(preview.importId(), preview.applicationId()).orElseThrow(() -> failure(null));
    }

    private String transaction(MessageImportPreview preview, String environmentStateId, MessageImportReceipt receipt) {
        var app = recordIdLiteral("application", preview.applicationId());
        var sql = new StringBuilder("BEGIN TRANSACTION;");
        var receiptId = recordIdLiteral("message_import_receipt", preview.importId());
        require(sql, "!record::exists(" + receiptId + ")");
        require(sql, "record::exists(" + recordIdLiteral("message_environment_state", environmentStateId) + ")");
        for (var environment : preview.environmentIds()) {
            require(sql, "array::len((SELECT id FROM " + recordIdLiteral("environment", environment)
                    + " WHERE application_id = " + app + ")) = 1");
        }
        var checked = new HashSet<String>();
        for (var row : preview.rows()) {
            var message = row.message();
            if (checked.add(message.getFunctionalityId())) {
                require(sql, "array::len((SELECT id FROM " + recordIdLiteral("functionality", message.getFunctionalityId())
                        + " WHERE application_id = " + app + " AND string::lowercase(state_id.name) IN ['activo', 'active'])) = 1");
            }
            appendMessage(sql, row, preview, environmentStateId, app);
        }
        try {
            sql.append("CREATE ONLY ").append(receiptId).append(" CONTENT { application_id: ").append(app)
                    .append(", receipt_json: ").append(quote(json.writeValueAsString(receipt))).append(" };");
        } catch (JsonProcessingException ex) {
            throw failure(ex);
        }
        return sql.append("COMMIT TRANSACTION;").toString();
    }

    private void appendMessage(StringBuilder sql, ImportRowResult row, MessageImportPreview preview,
                                String environmentStateId, String app) {
        var dto = row.message();
        var type = recordIdLiteral("message_type", dto.getTypeId());
        var category = recordIdLiteral("message_category", dto.getCategoryId());
        var state = recordIdLiteral("message_state", dto.getStatusId());
        require(sql, "record::exists(" + type + ") AND record::exists(" + category + ") AND record::exists(" + state + ")");
        var id = recordIdLiteral("message", UUID.randomUUID().toString());
        sql.append("CREATE ONLY ").append(id).append(" CONTENT { code: ").append(quote(row.normalizedCode()))
                .append(", title: ").append(quote(dto.getTitle())).append(", content: ").append(quote(dto.getContent()))
                .append(", application_id: ").append(app)
                .append(", functionality_id: ").append(recordIdLiteral("functionality", dto.getFunctionalityId()))
                .append(", type_id: ").append(type).append(", category_id: ").append(category)
                .append(", status_id: ").append(state).append(" };");
        for (var environment : preview.environmentIds()) {
            sql.append("CREATE ONLY ").append(recordIdLiteral("message_environment", UUID.randomUUID().toString()))
                    .append(" CONTENT { message_id: ").append(id)
                    .append(", environment_id: ").append(recordIdLiteral("environment", environment))
                    .append(", state_data_id: ").append(recordIdLiteral("message_environment_state", environmentStateId)).append(" };");
        }
    }

    private void require(StringBuilder sql, String condition) {
        sql.append("IF !( ").append(condition).append(" ) { THROW ")
                .append(quote(catalog.getMessage(IMP_010.getCode()))).append("; }; ");
    }

    private BusinessException failure(Exception ex) {
        return BusinessException.buildTechnicalException(catalog.getMessage(TCH_079.getCode()), ex, ExceptionLocation.INFRASTRUCTURE);
    }
}
