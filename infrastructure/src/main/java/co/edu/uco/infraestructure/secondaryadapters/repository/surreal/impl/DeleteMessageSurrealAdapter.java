package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class DeleteMessageSurrealAdapter implements DeleteMessageRepository {

    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_MESSAGE_ENVIRONMENT = "message_environment";

    private final LoggingPort log;
    private final Surreal surreal;

    public DeleteMessageSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(DeleteMessageSurrealAdapter.class);
        this.surreal = surreal;
    }

    @Override
    public void deleteMessage(String messageId) {
        String cleanId = getDefault(messageId).trim();

        String deleteEnvSql = "DELETE " + TABLE_MESSAGE_ENVIRONMENT
                + " WHERE message_id = " + toRecordId(TABLE_MESSAGE, cleanId) + ";";

        String deleteMessageSql = "DELETE " + toRecordId(TABLE_MESSAGE, cleanId) + ";";

        String transactionSql = "BEGIN TRANSACTION;"
                + deleteEnvSql
                + deleteMessageSql
                + "COMMIT TRANSACTION;";

        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_100.getCode()), cleanId);
            surreal.query(transactionSql);
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_074.getCode()), ex);
            throw BusinessException.buildTechnicalException(
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_075.getCode()),
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
