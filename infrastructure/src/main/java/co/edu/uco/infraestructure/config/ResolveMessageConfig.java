package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.common.catalog.strategy.cache.CacheCatalog;
import co.edu.uco.application.common.catalog.strategy.database.DatabaseCatalog;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.ResolveMessageUseCase;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResolveMessageConfig {

    @Bean
    public MessageCatalogStrategy messageCatalogStrategy(
            CacheCatalog cacheCatalog, DatabaseCatalog databaseCatalog, CatalogPort catalogPort,
            LoggingPortFactory loggerFactory, ResolveMessageRepository repository) {
        return new MessageCatalogStrategy(cacheCatalog, databaseCatalog, catalogPort, loggerFactory, repository);
    }

    @Bean
    public ResolveMessageUseCaseFacade resolveMessageUseCaseFacade(
            FindMessageCodeValidator codeValidator, MessageEnvironmentResolver environmentResolver,
            MessageCatalogStrategy messageCatalogStrategy, CatalogPort catalogPort) {
        return new ResolveMessageUseCase(codeValidator, environmentResolver, messageCatalogStrategy, catalogPort);
    }
}
