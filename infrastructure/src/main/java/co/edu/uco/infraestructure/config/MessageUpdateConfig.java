package co.edu.uco.infraestructure.config;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.application.usecase.UpdateMessageUseCase;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.UpdateMessageCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageUpdateConfig {
    @Bean
    public UpdateMessageUseCase updateMessageUseCase(UpdateMessageRepository updates, DataBaseMessageRepository messages,
            UpdateMessageCompositeValidator validator, MessageEnvironmentResolver access,
            MessageEnvironmentStateCatalogRepository environmentStates, MessageCatalogStrategy messageCatalog,
            CatalogPort catalog, LoggingPortFactory logging) {
        return new UpdateMessageUseCase(updates, messages, validator, access, environmentStates, messageCatalog,
                catalog, logging);
    }
}
