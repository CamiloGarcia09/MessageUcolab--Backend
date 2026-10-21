package co.edu.uco.application.common.catalog.strategy;

import co.edu.uco.application.common.catalog.strategy.cache.CacheCatalog;
import co.edu.uco.application.common.catalog.strategy.database.DatabaseCatalog;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public final class MessageCatalogStrategy {

    private final CacheCatalog cacheCatalog;
    private final DatabaseCatalog databaseCatalog;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public MessageCatalogStrategy(CacheCatalog cacheCatalog, DatabaseCatalog databaseCatalog,
            CatalogPort catalogPort, LoggingPortFactory loggerFactory) {
        this.cacheCatalog = cacheCatalog;
        this.databaseCatalog = databaseCatalog;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(MessageCatalogStrategy.class);
    }

    public SimplePage<MessageData> getMessagesWithEnvironment(String environment, SimplePageRequest request) {
        if (request.getFilter() != null && request.getFilter().hasFilters()) {
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode()));
            var dbMessages = databaseCatalog.getMessageWithEnvironment(environment, request);
            if (dbMessages.getData().isEmpty()) {
                return SimplePage.of(List.of(), request.getPage(), request.getSize(), 0, 1);
            }
            return dbMessages;
        }

        var cachedMessages = getCachedMessagesWithEnvironment(environment, request);
        if (cachedMessages.getData().isEmpty()) {
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode()));
            var dbMessages = databaseCatalog.getMessageWithEnvironment(environment, request);
            if (!dbMessages.getData().isEmpty()) {
                log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_007.getCode()));
                fillCacheWithEnvironmentMessages(dbMessages, environment);
                return dbMessages;
            }
            throw NotFoundException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_009.getCode()).formatted(environment));
        }

        var dbMessages = databaseCatalog.getMessageWithEnvironment(environment, request);
        if (!dbMessages.getData().isEmpty()) {
            if (cachedMessages.getTotalItems() != dbMessages.getTotalItems()) {
                log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_008.getCode()));
                fillCacheWithEnvironmentMessages(dbMessages, environment);
                return dbMessages;
            }
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_009.getCode()));
            return SimplePage.of(cachedMessages.getData(), cachedMessages.getPage(), cachedMessages.getSize(),
                    dbMessages.getTotalItems(), dbMessages.getTotalPages());
        }
        throw NotFoundException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_009.getCode()).formatted(environment));
    }

    public Optional<MessageData> getMessageByCodeAndEnvironment(String code, String environmentId) {
        var response = cacheCatalog.getMessageByCodeAndEnvironment(code, environmentId);
        if (response.isPresent()) {
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_009.getCode()));
        }
        if (response.isEmpty()) {
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_006.getCode()));
            response = databaseCatalog.getMessageByCodeAndEnvironment(code, environmentId);
        }
        return response;
    }

    private SimplePage<MessageData> getCachedMessagesWithEnvironment(String environment, SimplePageRequest request) {
        try {
            return cacheCatalog.getMessageWithEnvironment(environment, request);
        } catch (Exception exception) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_013.getCode()), exception);
            return SimplePage.of(List.of(), request.getPage(), request.getSize(), 0, 0);
        }
    }

    private void fillCacheWithEnvironmentMessages(SimplePage<MessageData> dbMessages, String environment) {
        dbMessages.getData().forEach(message -> cacheCatalog.addMessageWithEnvironment(message, environment));
    }

    // La lista por aplicacion combina ambientes y filtros: se consulta siempre la base de datos.
    public SimplePage<MessageData> getMessagesWithApplication(String applicationId, SimplePageRequest request) {
        return databaseCatalog.getMessagesWithApplication(applicationId, request);
    }

    public Optional<MessageData> getMessageByCodeAndApplication(String code, String applicationId) {
        return databaseCatalog.getMessageByCodeAndApplication(code, applicationId);
    }

    public void evictMessage(String code) {
        cacheCatalog.evictMessage(code);
    }

    public String getSystemMessageContent(String code) {
        return catalogPort.getMessage(code);
    }
}