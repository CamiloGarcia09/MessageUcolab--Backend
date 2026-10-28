package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.CreateMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class CreateMessageSurrealAdapter implements CreateMessageRepository {

    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_MESSAGE_ENVIRONMENT = "message_environment";
    private static final String TABLE_APPLICATION = "application";
    private static final String TABLE_FUNCTIONALITY = "functionality";
    private static final String TABLE_ENVIRONMENT = "environment";
    private static final String TABLE_MESSAGE_TYPE = "message_type";
    private static final String TABLE_MESSAGE_CATEGORY = "message_category";
    private static final String TABLE_MESSAGE_STATE = "message_state";
    private static final String TABLE_MESSAGE_ENV_STATE = "message_environment_state";

    private final LoggingPort log;
    private final Surreal surreal;

    public CreateMessageSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(CreateMessageSurrealAdapter.class);
        this.surreal = surreal;
    }

    @Override
    public void createMessage(MessageData message, List<String> environmentIds, String messageEnvironmentStateId) {
        String messageId = message.getId().toString();

        String appId = message.getFunctionality().getApplication().getId().toString();
        String funcId = message.getFunctionality().getId().toString();
        String typeId = message.getType().getId().toString();
        String categoryId = message.getCategory().getId().toString();
        String statusId = message.getStatus().getId().toString();

        String upsertMessageSql = "UPSERT " + toRecordId(TABLE_MESSAGE, messageId) + " CONTENT { "
                + "code: " + quote(message.getCode()) + ", "
                + "title: " + quote(message.getTitle()) + ", "
                + "content: " + quote(message.getContent()) + ", "
                + "type_id: " + toRecordId(TABLE_MESSAGE_TYPE, typeId) + ", "
                + "category_id: " + toRecordId(TABLE_MESSAGE_CATEGORY, categoryId) + ", "
                + "status_id: " + toRecordId(TABLE_MESSAGE_STATE, statusId) + ", "
                + "application_id: " + toRecordId(TABLE_APPLICATION, appId) + ", "
                + "functionality_id: " + toRecordId(TABLE_FUNCTIONALITY, funcId)
                + " };";

        // Un mismo mensaje enlazado a cada ambiente elegido; todo o nada.
        StringBuilder upsertMessageEnvSql = new StringBuilder();
        for (String environmentId : environmentIds) {
            upsertMessageEnvSql.append("UPSERT ")
                    .append(toRecordId(TABLE_MESSAGE_ENVIRONMENT, UtilUUID.getNewUUID().toString()))
                    .append(" CONTENT { ")
                    .append("message_id: ").append(toRecordId(TABLE_MESSAGE, messageId)).append(", ")
                    .append("environment_id: ").append(toRecordId(TABLE_ENVIRONMENT, environmentId)).append(", ")
                    .append("state_data_id: ").append(toRecordId(TABLE_MESSAGE_ENV_STATE, messageEnvironmentStateId))
                    .append(" };");
        }

        String transactionSql = "BEGIN TRANSACTION;"
                + upsertMessageSql
                + upsertMessageEnvSql
                + "COMMIT TRANSACTION;";

        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_099.getCode()), messageId);
            surreal.query(transactionSql);
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_066.getCode()), ex);
            throw BusinessException.buildTechnicalException(
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_067.getCode()),
                    ex,
                    ExceptionLocation.INFRASTRUCTURE
            );
        }
    }

    private static String toRecordId(String table, String id) {
        String clean = getDefault(id).trim();
        if (clean.startsWith(table + ":")) {
            clean = clean.substring(table.length() + 1);
        }
        return recordIdLiteral(table, clean);
    }
}
