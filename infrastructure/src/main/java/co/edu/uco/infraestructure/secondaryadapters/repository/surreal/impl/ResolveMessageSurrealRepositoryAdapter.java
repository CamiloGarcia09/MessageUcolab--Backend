package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageAvailabilityData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

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

    private static final String MESSAGE_PROJECTION = "message_id AS id, message_id.code AS code, "
            + "message_id.title AS title, message_id.content AS content, message_id.type_id.name AS type, "
            + "message_id.category_id.name AS category, message_id.functionality_id.name AS functionality, "
            + "message_id.application_id AS application_id, message_id.created_at AS created_at, "
            + "message_id.updated_at AS updated_at";

    @Override
    public Optional<MessageData> findAvailableByCodeAndEnvironment(String code, String environmentId) {
        return findAvailable(MESSAGE_PROJECTION, code, environmentId,
                ResolveMessageSurrealRepositoryAdapter::toMessageData);
    }

    @Override
    public List<MessageData> findAvailableByEnvironment(String environmentId) {
        var sql = "SELECT " + MESSAGE_PROJECTION + " FROM message_environment"
                + availableIn(environmentId) + " ORDER BY code;";
        var errorMessage = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_058.getCode()).formatted(sql);
        try {
            return query(sql, errorMessage, ResolveMessageSurrealRepositoryAdapter::toMessageData);
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw CrossWordsException.buildInfrastructure(errorMessage, exception);
        }
    }

    @Override
    public Optional<MessageAvailabilityData> findAvailabilityByCodeAndEnvironment(String code, String environmentId) {
        return findAvailable("message_id AS id, message_id.updated_at AS updated_at", code, environmentId,
                object -> new MessageAvailabilityData(extractIdAsUUID(object.get("id")),
                        instantOf(object.get("updated_at"))));
    }

    private <T> Optional<T> findAvailable(String projection, String code, String environmentId, RowMapper<T> mapper) {
        var sql = "SELECT " + projection + " FROM message_environment" + availableIn(environmentId)
                + " AND message_id.code = " + quote(code)
                + " LIMIT 1;";
        var errorMessage = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_058.getCode()).formatted(sql);
        try {
            return queryOne(sql, errorMessage, mapper);
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw CrossWordsException.buildInfrastructure(errorMessage, exception);
        }
    }

    // Mensaje activo, con asociacion activa al ambiente y de la misma aplicacion que el ambiente.
    private static String availableIn(String environmentId) {
        return " WHERE environment_id = " + recordIdLiteral("environment", environmentId)
                + " AND message_id.application_id = environment_id.application_id"
                + " AND string::lowercase(message_id.status_id.name) IN ['activo', 'active']"
                + " AND string::lowercase(state_data_id.name) IN ['activo', 'active']";
    }

    private static MessageData toMessageData(Object object) {
        var message = MessageData.build();
        message.setId(extractIdAsUUID(object.get("id")));
        message.setCode(stringOf(object.get("code")));
        message.setTitle(stringOf(object.get("title")));
        message.setContent(stringOf(object.get("content")));
        message.setApplication(extractIdAsUUID(object.get("application_id")).toString());
        message.setType(MessageTypeData.build(stringOf(object.get("type"))));
        message.setCategory(MessageCategoryData.build(stringOf(object.get("category"))));
        message.setFunctionality(FunctionalityData.build(stringOf(object.get("functionality"))));
        message.setCreatedAt(instantOf(object.get("created_at")));
        message.setUpdatedAt(instantOf(object.get("updated_at")));
        return message;
    }

    private static Instant instantOf(Value value) {
        if (value == null || value.isNull() || value.isNone()) {
            return null;
        }
        if (value.isDateTime()) {
            return value.getDateTime().toInstant();
        }
        return Instant.parse(value.getString());
    }
}
