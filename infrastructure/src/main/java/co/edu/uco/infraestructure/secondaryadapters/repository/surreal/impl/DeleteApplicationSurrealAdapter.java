package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_231;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_107;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class DeleteApplicationSurrealAdapter extends SurrealCatalogSupport implements DeleteApplicationRepository {
    private final CatalogPort catalog;

    public DeleteApplicationSurrealAdapter(Surreal surreal, LoggingPortFactory logging, CatalogPort catalog) {
        super(surreal, logging.getLogger(DeleteApplicationSurrealAdapter.class));
        this.catalog = catalog;
    }

    @Override
    public boolean ownsApplication(UUID userId, UUID applicationId) {
        try {
            return queryOne("SELECT id FROM " + recordIdLiteral("application", applicationId.toString())
                    + " WHERE organization_id.owner_user_id = " + recordIdLiteral("app_user", userId.toString())
                    + ";", catalog.getMessage(TCH_107.getCode()), row -> true).orElse(false);
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<UUID> deleteAtomically(UUID userId, UUID applicationId) {
        String sql = transaction(userId, applicationId);
        try {
            return deletedMessageIds(surreal.query(sql));
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    private String transaction(UUID userId, UUID applicationId) {
        String application = recordIdLiteral("application", applicationId.toString());
        String owner = recordIdLiteral("app_user", userId.toString());
        // Snapshot dependent identifiers before deleting them. No shared catalog or organization is deleted.
        return "BEGIN TRANSACTION;"
                + "LET $app = " + application + ";"
                + "LET $owned = (SELECT id FROM $app WHERE organization_id.owner_user_id = " + owner + ");"
                + "IF array::len($owned) != 1 { THROW " + quote(catalog.getMessage(FUN_231.getCode())) + "; };"
                + "LET $environments = (SELECT VALUE id FROM environment WHERE application_id = $app);"
                + "LET $messages = (SELECT id FROM message WHERE application_id = $app);"
                + "LET $message_refs = (SELECT VALUE <string> id FROM message WHERE application_id = $app);"
                + "LET $environment_refs = (SELECT VALUE <string> id FROM environment WHERE application_id = $app);"
                + deleteProjections()
                + "DELETE message_environment WHERE message_id IN $messages.id"
                + " OR environment_id IN $environments RETURN NONE;"
                + "DELETE message WHERE application_id = $app RETURN NONE;"
                + "DELETE functionality WHERE application_id = $app RETURN NONE;"
                + "DELETE message_import_receipt WHERE application_id = $app RETURN NONE;"
                + "DELETE environment WHERE application_id = $app RETURN NONE;"
                + "DELETE $app RETURN NONE;"
                + "RETURN $messages;"
                + "COMMIT TRANSACTION;";
    }

    private static String deleteProjections() {
        // Existing event consumers also remove these projections, including when replaying older pending events.
        return "DELETE message_environment_readmodel WHERE message_id IN $message_refs"
                + " OR environment_id IN $environment_refs RETURN NONE;"
                + "DELETE message_data_collection WHERE application_id = <string>$app"
                + " OR message_id IN $message_refs RETURN NONE;"
                + "DELETE environment_document WHERE application_id = <string>$app"
                + " OR environment_id IN $environment_refs RETURN NONE;"
                + "DELETE application_document WHERE application_id = <string>$app RETURN NONE;";
    }

    private static List<UUID> deletedMessageIds(Response response) {
        List<UUID> ids = new ArrayList<>();
        // Taking every result is required: the native driver may defer transaction errors until then.
        for (int index = 0; index < response.size(); index++) {
            var result = response.take(index);
            if (result == null || !result.isArray()) {
                continue;
            }
            var rows = result.getArray();
            for (int row = 0; row < rows.len(); row++) {
                var value = rows.get(row);
                if (value != null && value.isObject()) {
                    ids.add(extractIdAsUUID(value.getObject().get("id")));
                }
            }
        }
        return List.copyOf(ids);
    }

    private BusinessException failure(RuntimeException exception) {
        String message = catalog.getMessage(TCH_107.getCode());
        log.error(message, exception);
        return BusinessException.buildTechnicalException(message, exception, ExceptionLocation.INFRASTRUCTURE);
    }
}
