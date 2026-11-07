package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_226;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_106;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class UpdateFunctionalityStateSurrealAdapter extends SurrealCatalogSupport
        implements UpdateFunctionalityStateRepository {
    private final CatalogPort catalog;

    public UpdateFunctionalityStateSurrealAdapter(Surreal surreal, LoggingPortFactory logging, CatalogPort catalog) {
        super(surreal, logging.getLogger(UpdateFunctionalityStateSurrealAdapter.class));
        this.catalog = catalog;
    }

    @Override
    public boolean existsInApplication(String functionalityId, String applicationId) {
        return find("SELECT id FROM " + recordIdLiteral("functionality", functionalityId)
                        + " WHERE application_id = " + recordIdLiteral("application", applicationId) + ";",
                row -> true).isPresent();
    }

    @Override
    public Optional<String> findStateNameById(String stateId) {
        return find("SELECT name FROM " + recordIdLiteral("functionality_state", stateId) + ";",
                row -> stringOf(row.get("name")));
    }

    @Override
    public List<String> updateState(String functionalityId, String applicationId, String stateId,
                                     String inactiveMessageStateId) {
        String functionality = recordIdLiteral("functionality", functionalityId);
        String application = recordIdLiteral("application", applicationId);
        String cascade = inactiveMessageStateId == null ? "RETURN [];" :
                cascadeSql(functionality, application, inactiveMessageStateId);
        String sql = "BEGIN TRANSACTION;"
                + "LET $owned = (SELECT id FROM " + functionality + " WHERE application_id = " + application + ");"
                + "IF array::len($owned) != 1 { THROW " + quote(catalog.getMessage(FUN_226.getCode())) + "; };"
                + "UPDATE " + functionality + " SET state_id = " + recordIdLiteral("functionality_state", stateId)
                + " WHERE application_id = " + application + " RETURN NONE;"
                + cascade + "COMMIT TRANSACTION;";
        try {
            return changedCodes(surreal.query(sql));
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    private static String cascadeSql(String functionality, String application, String inactiveMessageStateId) {
        String inactive = recordIdLiteral("message_state", inactiveMessageStateId);
        String targets = "functionality_id = " + functionality + " AND application_id = " + application
                + " AND status_id != " + inactive;
        String pendingEvents = "aggregate_type = 'message' AND event_type = 'MESSAGE_UPDATED'"
                + " AND aggregate_id IN $cascade_message_ids"
                + " AND (projection_status = NONE OR projection_status != 'processed')";
        // Preserve older pending edits; only mark trigger events created by this transaction as collective.
        return "LET $cascade_message_ids = (SELECT VALUE <string> id FROM message WHERE " + targets + ");"
                + "LET $previous_message_events = (SELECT VALUE id FROM domain_events WHERE " + pendingEvents + ");"
                + "LET $changed = (UPDATE message SET status_id = " + inactive + " WHERE " + targets + " RETURN code);"
                + "UPDATE domain_events SET metadata.notification_type = 'MessagesChanged' WHERE " + pendingEvents
                + " AND id NOT IN $previous_message_events RETURN NONE;"
                + "RETURN $changed;";
    }

    private <T> Optional<T> find(String sql, RowMapper<T> mapper) {
        try {
            return queryOne(sql, catalog.getMessage(TCH_106.getCode()), mapper);
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    private BusinessException failure(RuntimeException exception) {
        String message = catalog.getMessage(TCH_106.getCode());
        log.error(message, exception);
        return BusinessException.buildTechnicalException(message, exception, ExceptionLocation.INFRASTRUCTURE);
    }

    private static List<String> changedCodes(Response response) {
        List<String> codes = new ArrayList<>();
        // Consume every statement: the driver surfaces transaction errors when taking results.
        for (int index = 0; index < response.size(); index++) {
            appendCodes(response.take(index), codes);
        }
        return codes;
    }

    private static void appendCodes(Value result, List<String> codes) {
        if (result == null || !result.isArray()) {
            return;
        }
        var rows = result.getArray();
        for (int row = 0; row < rows.len(); row++) {
            Value item = rows.get(row);
            if (item != null && item.isObject()) {
                String code = stringOf(item.getObject().get("code"));
                if (!code.isBlank()) {
                    codes.add(code);
                }
            }
        }
    }
}
