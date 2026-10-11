package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class ResolveMessageSurrealRepositoryAdapter extends SurrealCatalogSupport
        implements ResolveMessageRepository {

    private final CatalogPort catalogPort;

    public ResolveMessageSurrealRepositoryAdapter(Surreal surreal, LoggingPortFactory loggerFactory,
                                                   CatalogPort catalogPort) {
        super(surreal, loggerFactory.getLogger(ResolveMessageSurrealRepositoryAdapter.class));
        this.catalogPort = catalogPort;
    }

    @Override
    public Optional<MessageData> findAvailableByCodeAndEnvironment(String code, String environmentId) {
        var sql = "SELECT message_id.code AS code, message_id.title AS title, message_id.content AS content"
                + " FROM message_environment"
                + " WHERE environment_id = " + recordIdLiteral("environment", environmentId)
                + " AND message_id.code = " + quote(code)
                + " AND message_id.application_id = environment_id.application_id"
                + " AND message_id.status_id.name = 'Active'"
                + " AND state_data_id.name = 'Active'"
                + " LIMIT 1;";
        var errorMessage = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_058.getCode()).formatted(sql);
        try {
            return queryOne(sql, errorMessage, ResolveMessageSurrealRepositoryAdapter::toMessageData);
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw CrossWordsException.buildInfrastructure(errorMessage, exception);
        }
    }

    private static MessageData toMessageData(Object object) {
        var message = MessageData.build();
        message.setCode(stringOf(object.get("code")));
        message.setTitle(stringOf(object.get("title")));
        message.setContent(stringOf(object.get("content")));
        return message;
    }
}
