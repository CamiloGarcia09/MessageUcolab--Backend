package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.primaryports.facade.functionality.CreateFunctionalityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.functionality.impl.CreateFunctionalityUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.usecase.CreateFunctionalityUseCase;
import co.edu.uco.application.usecase.handling.HandlingCreateFunctionalityPort;
import co.edu.uco.application.usecase.validator.functionality.CreateFunctionalityCompositeValidator;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.primaryports.facade.application.CreateApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.application.impl.CreateApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.primaryports.facade.catalog.impl.FindCatalogUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.organization.CreateOrganizationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.organization.impl.CreateOrganizationUseCaseFacadeImpl;
import co.edu.uco.application.usecase.CreateApplicationUseCase;
import co.edu.uco.application.usecase.FindCatalogUseCase;
import co.edu.uco.application.usecase.CreateOrganizationUseCase;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingCreateOrganizationPort;
import co.edu.uco.application.usecase.handling.HandlingFindCatalogPort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolverImpl;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.application.usecase.validator.message.UpdateMessageCompositeValidator;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.organization.CreateOrganizationCompositeValidator;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.impl.SessionApplicationUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.security.SecurityApplicationPort;
import co.edu.uco.application.usecase.SessionApplicationUseCase;
import co.edu.uco.application.usecase.handling.HandlingSessionApplicationPort;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationRegistrationCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class UseCaseRuleConfig {

    @Bean
    HandlingCreateFunctionalityPort handlingCreateFunctionalityPort(
            FunctionalityRepository functionalityRepository,
            CreateFunctionalityCompositeValidator validator,
            LoggingPortFactory loggerFactory) {
        return new CreateFunctionalityUseCase(functionalityRepository, validator, loggerFactory);
    }

    @Bean
    CreateFunctionalityUseCaseFacade createFunctionalityUseCaseFacade(HandlingCreateFunctionalityPort port) {
        return new CreateFunctionalityUseCaseFacadeImpl(port);
    }

    @Bean
    SessionApplicationCompositeValidator sessionApplicationCompositeValidator(
            CatalogPort catalogPort,
            ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository) {
        return new SessionApplicationCompositeValidator(catalogPort, applicationRepository, environmentRepository);
    }

    @Bean
    SessionApplicationAccessCompositeValidator sessionApplicationAccessCompositeValidator(
            CatalogPort catalogPort,
            ApplicationRepository applicationRepository) {
        return new SessionApplicationAccessCompositeValidator(catalogPort, applicationRepository);
    }

    @Bean
    SessionApplicationRegistrationCompositeValidator sessionApplicationRegistrationCompositeValidator(
            CatalogPort catalogPort) {
        return new SessionApplicationRegistrationCompositeValidator(catalogPort);
    }

    @Bean
    HandlingSessionApplicationPort handlingSessionApplicationPort(
            SecurityApplicationPort securityApplicationPort,
            ApplicationRepository applicationRepository,
            SessionApplicationCompositeValidator sessionApplicationValidator,
            SessionApplicationAccessCompositeValidator sessionApplicationAccessValidator,
            SessionApplicationRegistrationCompositeValidator sessionApplicationRegistrationValidator,
            CatalogPort catalogPort) {
        return new SessionApplicationUseCase(
                securityApplicationPort,
                applicationRepository,
                sessionApplicationValidator,
                sessionApplicationAccessValidator,
                sessionApplicationRegistrationValidator,
                catalogPort);
    }

    @Bean
    SessionApplicationUseCaseFacade sessionApplicationUseCaseFacade(
            HandlingSessionApplicationPort handlingSessionApplicationPort) {
        return new SessionApplicationUseCaseFacadeImpl(handlingSessionApplicationPort);
    }

    @Bean
    MessageEnvironmentResolver messageEnvironmentResolver(
            MessageEnvironmentSelectionCompositeValidator environmentSelectionValidator,
            CatalogPort catalogPort) {
        return new MessageEnvironmentResolverImpl(
                environmentSelectionValidator, catalogPort);
    }

    @Bean
    HandlingFindCatalogPort handlingFindCatalogPort(
            ApplicationCatalogRepository applicationCatalogRepository,
            EnvironmentCatalogRepository environmentCatalogRepository,
            FunctionalityCatalogRepository functionalityCatalogRepository,
            MessageTypeCatalogRepository messageTypeCatalogRepository,
            MessageCategoryCatalogRepository messageCategoryCatalogRepository,
            MessageStateCatalogRepository messageStateCatalogRepository,
            MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository,
            SessionApplicationAccessCompositeValidator accessValidator) {
        return new FindCatalogUseCase(
                applicationCatalogRepository,
                environmentCatalogRepository,
                functionalityCatalogRepository,
                messageTypeCatalogRepository,
                messageCategoryCatalogRepository,
                messageStateCatalogRepository,
                messageEnvironmentStateCatalogRepository,
                accessValidator);
    }

    @Bean
    FindCatalogUseCaseFacade findCatalogUseCaseFacade(HandlingFindCatalogPort handlingFindCatalogPort) {
        return new FindCatalogUseCaseFacadeImpl(handlingFindCatalogPort);
    }

    @Bean
    CreateMessageCompositeValidator createMessageCompositeValidator(
            CatalogPort catalogPort,
            RecordExistsCatalogPort recordExistsCatalogPort,
            FunctionalityCatalogRepository functionalityCatalogRepository,
            MessageCodeQueryPort messageCodeQueryPort,
            MessageEnvironmentSelectionCompositeValidator messageEnvironmentSelectionValidator) {
        return new CreateMessageCompositeValidator(
                catalogPort, recordExistsCatalogPort, functionalityCatalogRepository, messageCodeQueryPort,
                messageEnvironmentSelectionValidator);
    }

    @Bean
    MessageEnvironmentSelectionCompositeValidator messageEnvironmentSelectionCompositeValidator(
            CatalogPort catalogPort,
            EnvironmentRepository environmentRepository) {
        return new MessageEnvironmentSelectionCompositeValidator(catalogPort, environmentRepository);
    }

    @Bean
    UpdateMessageCompositeValidator updateMessageCompositeValidator(
            CatalogPort catalogPort,
            RecordExistsCatalogPort recordExistsCatalogPort,
            FunctionalityCatalogRepository functionalityCatalogRepository,
            MessageEnvironmentSelectionCompositeValidator messageEnvironmentSelectionValidator) {
        return new UpdateMessageCompositeValidator(
                catalogPort, recordExistsCatalogPort, functionalityCatalogRepository,
                messageEnvironmentSelectionValidator);
    }

    @Bean
    CreateOrganizationCompositeValidator createOrganizationCompositeValidator(
            CatalogPort catalogPort,
            OrganizationRepository organizationRepository) {
        return new CreateOrganizationCompositeValidator(catalogPort, organizationRepository);
    }

    @Bean
    HandlingCreateOrganizationPort handlingCreateOrganizationPort(
            OrganizationRepository organizationRepository,
            CreateOrganizationCompositeValidator validator,
            LoggingPortFactory loggerFactory) {
        return new CreateOrganizationUseCase(organizationRepository, validator, loggerFactory);
    }

    @Bean
    CreateOrganizationUseCaseFacade createOrganizationUseCaseFacade(
            HandlingCreateOrganizationPort handlingCreateOrganizationPort) {
        return new CreateOrganizationUseCaseFacadeImpl(handlingCreateOrganizationPort);
    }

    @Bean
    CreateApplicationCompositeValidator createApplicationCompositeValidator(
            CatalogPort catalogPort,
            RecordExistsCatalogPort recordExistsCatalogPort,
            ApplicationRepository applicationRepository,
            OrganizationRepository organizationRepository) {
        return new CreateApplicationCompositeValidator(
                catalogPort,
                recordExistsCatalogPort,
                applicationRepository,
                organizationRepository);
    }

    @Bean
    HandlingCreateApplicationPort handlingCreateApplicationPort(
            ApplicationRepository applicationRepository,
            EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository,
            CreateApplicationCompositeValidator validator,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory) {
        return new CreateApplicationUseCase(
                applicationRepository, environmentReferenceCatalogRepository, validator, catalogPort, loggerFactory);
    }

    @Bean
    CreateApplicationUseCaseFacade createApplicationUseCaseFacade(
            HandlingCreateApplicationPort handlingCreateApplicationPort) {
        return new CreateApplicationUseCaseFacadeImpl(handlingCreateApplicationPort);
    }
}
