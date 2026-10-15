package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class UpdateMessageSurrealAdapter implements UpdateMessageRepository {

    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_MESSAGE_TYPE = "message_type";
    private static final String TABLE_MESSAGE_CATEGORY = "message_category";
    private static final String TABLE_MESSAGE_STATE = "message_state";
    private static final String TABLE_FUNCTIONALITY = "functionality";

    private final LoggingPort log;
    private final Surreal surreal;

    public UpdateMessageSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(UpdateMessageSurrealAdapter.class);
        this.surreal = surreal;
    }

    @Override
    public void updateMessage(MessageData message) {
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

        try {
            log.info("Updating message with id: {}", messageId);
            surreal.query(updateSql);
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_072.getCode()), ex);
            throw BusinessException.buildTechnicalException(
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_073.getCode()),
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
