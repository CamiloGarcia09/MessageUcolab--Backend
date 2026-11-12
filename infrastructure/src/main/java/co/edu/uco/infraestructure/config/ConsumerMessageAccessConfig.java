package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.message.ListConsumerMessagesUseCaseFacade;
import co.edu.uco.application.primaryports.facade.serviceidentity.ServiceIdentityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.serviceidentity.impl.ServiceIdentityUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.application.usecase.ListConsumerMessagesUseCase;
import co.edu.uco.application.usecase.ManageServiceIdentitiesUseCase;
import co.edu.uco.application.usecase.security.ConsumerMessageAccessResolver;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityClientIdValidator;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityEnvironmentCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Consumo de mensajes por aplicaciones consumidoras que llaman con su identidad de servicio, y registro de
 * esas identidades en los ambientes de cada aplicacion.
 */
@Configuration
public class ConsumerMessageAccessConfig {

    @Bean
    public ConsumerMessageAccessResolver consumerMessageAccessResolver(
            ServiceIdentityRepository identities, ConsumerAuthorizationPort authorization, CatalogPort catalogPort) {
        return new ConsumerMessageAccessResolver(identities, authorization, catalogPort);
    }

    @Bean
    public ServiceIdentityUseCaseFacade serviceIdentityUseCaseFacade(
            ServiceIdentityRepository identities, CatalogPort catalogPort,
            ApplicationRepository applicationRepository, EnvironmentRepository environmentRepository) {
        var environmentValidator = new ServiceIdentityEnvironmentCompositeValidator(
                catalogPort, applicationRepository, environmentRepository);
        return new ServiceIdentityUseCaseFacadeImpl(new ManageServiceIdentitiesUseCase(
                identities, environmentValidator, new ServiceIdentityClientIdValidator(catalogPort), catalogPort));
    }

    @Bean
    public ListConsumerMessagesUseCaseFacade listConsumerMessagesUseCaseFacade(
            MessageEnvironmentResolver environmentResolver, ResolveMessageRepository repository,
            ApplicationLanguageRepository languages) {
        return new ListConsumerMessagesUseCase(environmentResolver, repository, languages);
    }
}
