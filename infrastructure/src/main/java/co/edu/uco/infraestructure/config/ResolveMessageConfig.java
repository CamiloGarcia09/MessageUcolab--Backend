package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
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
    public ResolveMessageUseCaseFacade resolveMessageUseCaseFacade(
            FindMessageCodeValidator codeValidator, MessageEnvironmentResolver environmentResolver,
            ResolveMessageRepository repository, CatalogPort catalogPort) {
        return new ResolveMessageUseCase(codeValidator, environmentResolver, repository, catalogPort);
    }
}
