package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class UpdateMessageSurrealAdapter extends SurrealCatalogSupport implements UpdateMessageRepository {

    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_MESSAGE_TYPE = "message_type";
    private static final String TABLE_MESSAGE_CATEGORY = "message_category";
    private static final String TABLE_MESSAGE_STATE = "message_state";
    private static final String TABLE_FUNCTIONALITY = "functionality";
    private static final String TABLE_MESSAGE_ENVIRONMENT = "message_environment";
    private static final String TABLE_ENVIRONMENT = "environment";
    private static final String TABLE_MESSAGE_ENV_STATE = "message_environment_state";

    public UpdateMessageSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(UpdateMessageSurrealAdapter.class));
    }

    @Override
    public void updateMessage(MessageData message, List<String> environmentIds, String messageEnvironmentStateId) {
        String messageId = message.getId().toString();
        String typeId = message.getType().getId().toString();
        String categoryId = message.getCategory().getId().toString();
        String statusId = message.getStatus().getId().toString();
        String funcId = message.getFunctionality().getId().toString();

        String updateSql = "UPDATE " + toRecordId(TABLE_MESSAGE, messageId) + " MERGE { "
                + "title: " + quote(message.getTitle()) + ", "
                + "content: " + quote(message.getContent()) + ", "
                + "type_id: " + toRecordId(TABLE_MESSAGE_TYPE, typeId) + ", "
                + "category_id: " + toRecordId(TABLE_MESSAGE_CATEGORY, categoryId) + ", "
                + "status_id: " + toRecordId(TABLE_MESSAGE_STATE, statusId) + ", "
                + "functionality_id: " + toRecordId(TABLE_FUNCTIONALITY, funcId)
                + " };";

        executeUpdate(messageId, updateSql, environmentIds, messageEnvironmentStateId);
    }

    @Override
    public void updateEnvironments(String messageId, List<String> environmentIds, String messageEnvironmentStateId) {
        String updateSql = "UPDATE " + toRecordId(TABLE_MESSAGE, messageId) + " SET updated_at = time::now();";
        executeUpdate(messageId, updateSql, environmentIds, messageEnvironmentStateId);
    }

    private void executeUpdate(String messageId, String updateSql, List<String> environmentIds,
                               String messageEnvironmentStateId) {
        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_095.getCode()), messageId);
            if (environmentIds == null) {
                surreal.query(updateSql);
                return;
            }
            surreal.query("BEGIN TRANSACTION;"
                    + updateSql
                    + replaceEnvironmentsSql(messageId, environmentIds, messageEnvironmentStateId)
                    + "COMMIT TRANSACTION;");
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_072.getCode()), ex);
            throw BusinessException.buildTechnicalException(
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_073.getCode()),
                    ex,
                    ExceptionLocation.INFRASTRUCTURE
            );
        }
    }

    // Conserva las relaciones de los ambientes que siguen elegidos, borra las demas y crea las nuevas.
    private String replaceEnvironmentsSql(String messageId, List<String> environmentIds,
                                          String messageEnvironmentStateId) {
        Set<UUID> target = new LinkedHashSet<>();
        environmentIds.forEach(environmentId -> target.add(UtilUUID.getUUIDFromString(environmentId)));

        List<UUID[]> current = query("SELECT id, environment_id FROM " + TABLE_MESSAGE_ENVIRONMENT
                        + " WHERE message_id = " + toRecordId(TABLE_MESSAGE, messageId) + ";",
                "Error al consultar los ambientes del mensaje " + messageId,
                row -> new UUID[] {extractIdAsUUID(row.get("id")), extractIdAsUUID(row.get("environment_id"))});

        StringBuilder sql = new StringBuilder();
        Set<UUID> kept = new LinkedHashSet<>();
        for (UUID[] relation : current) {
            if (target.contains(relation[1]) && kept.add(relation[1])) {
                continue;
            }
            sql.append("DELETE ").append(toRecordId(TABLE_MESSAGE_ENVIRONMENT, relation[0].toString())).append(";");
        }
        for (UUID environmentId : target) {
            if (kept.contains(environmentId)) {
                continue;
            }
            sql.append("UPSERT ")
                    .append(toRecordId(TABLE_MESSAGE_ENVIRONMENT, UtilUUID.getNewUUID().toString()))
                    .append(" CONTENT { ")
                    .append("message_id: ").append(toRecordId(TABLE_MESSAGE, messageId)).append(", ")
                    .append("environment_id: ").append(toRecordId(TABLE_ENVIRONMENT, environmentId.toString())).append(", ")
                    .append("state_data_id: ").append(toRecordId(TABLE_MESSAGE_ENV_STATE, messageEnvironmentStateId))
                    .append(" };");
        }
        return sql.toString();
    }

    private static String toRecordId(String table, String id) {
        String clean = getDefault(id).trim();
        if (clean.startsWith(table + ":")) {
            clean = clean.substring(table.length() + 1);
        }
        return recordIdLiteral(table, clean);
    }
}
