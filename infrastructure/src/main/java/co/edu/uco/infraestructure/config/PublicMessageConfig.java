package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.message.FindPublicMessageUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.ListPublicMessagesUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.FindPublicMessageUseCase;
import co.edu.uco.application.usecase.ListPublicMessagesUseCase;
import co.edu.uco.application.usecase.security.PublicMessageAccessResolver;
import co.edu.uco.application.usecase.validator.consumer.PublicMessageAccessCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicMessageConfig {

    @Bean
    public PublicMessageAccessCompositeValidator publicMessageAccessCompositeValidator(
            CatalogPort catalogPort, ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository) {
        return new PublicMessageAccessCompositeValidator(catalogPort, applicationRepository, environmentRepository);
    }

    @Bean
    public PublicMessageAccessResolver publicMessageAccessResolver(
            PublicMessageAccessCompositeValidator validator, ConsumerAuthorizationPort authorization,
            CatalogPort catalogPort) {
        return new PublicMessageAccessResolver(validator, authorization, catalogPort);
    }

    @Bean
    public ListPublicMessagesUseCaseFacade listPublicMessagesUseCaseFacade(
            PublicMessageAccessResolver accessResolver, ResolveMessageRepository repository,
            ApplicationLanguageRepository languages) {
        return new ListPublicMessagesUseCase(accessResolver, repository, languages);
    }

    @Bean
    public FindPublicMessageUseCaseFacade findPublicMessageUseCaseFacade(
            PublicMessageAccessResolver accessResolver, TranslateConsumerMessageUseCaseFacade translator,
            ApplicationLanguageRepository languages) {
        return new FindPublicMessageUseCase(accessResolver, translator, languages);
    }
}
