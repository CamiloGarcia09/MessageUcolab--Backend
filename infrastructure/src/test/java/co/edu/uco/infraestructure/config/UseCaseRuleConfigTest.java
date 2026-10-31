package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.application.CreateApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.functionality.CreateFunctionalityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.primaryports.facade.organization.CreateOrganizationUseCaseFacade;
import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.usecase.CreateApplicationUseCase;
import co.edu.uco.application.usecase.CreateFunctionalityUseCase;
import co.edu.uco.application.usecase.CreateOrganizationUseCase;
import co.edu.uco.application.usecase.FindCatalogUseCase;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingCreateFunctionalityPort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolverImpl;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.functionality.CreateFunctionalityCompositeValidator;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.application.usecase.validator.organization.CreateOrganizationCompositeValidator;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UseCaseRuleConfigTest {
    @Mock private CatalogPort catalog;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock private MessageCodeQueryPort messageCodeQueryPort;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private EnvironmentRepository environmentRepository;
    @Mock private CreateApplicationCompositeValidator applicationValidator;
    @Mock private EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private FunctionalityRepository functionalityRepository;
    @Mock private CreateFunctionalityCompositeValidator functionalityValidator;
    @Mock private ApplicationCatalogRepository applicationCatalogRepository;
    @Mock private EnvironmentCatalogRepository environmentCatalogRepository;
    @Mock private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock private MessageTypeCatalogRepository messageTypeCatalogRepository;
    @Mock private MessageCategoryCatalogRepository messageCategoryCatalogRepository;
    @Mock private MessageStateCatalogRepository messageStateCatalogRepository;
    @Mock private MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository;

    private UseCaseRuleConfig config;

    @BeforeEach
    void setUp() {
        config = new UseCaseRuleConfig();
    }

    @Test
    void createApplicationBeansUseCurrentSessionIndependentUseCaseContracts() {
        when(loggerFactory.getLogger(CreateApplicationUseCase.class)).thenReturn(mock(LoggingPort.class));
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of(
                new co.edu.uco.application.secondaryports.entity.EnvironmentTypeData(
                        java.util.UUID.randomUUID(), "Develop"),
                new co.edu.uco.application.secondaryports.entity.EnvironmentTypeData(
                        java.util.UUID.randomUUID(), "Testing"),
                new co.edu.uco.application.secondaryports.entity.EnvironmentTypeData(
                        java.util.UUID.randomUUID(), "Production")));
        when(environmentReferenceCatalogRepository.findStateIdByName(co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE))
                .thenReturn(java.util.Optional.of(java.util.UUID.randomUUID()));
        HandlingCreateApplicationPort port = config.handlingCreateApplicationPort(applicationRepository,
                environmentReferenceCatalogRepository, applicationValidator, catalog, loggerFactory);
        CreateApplicationUseCaseFacade facade = config.createApplicationUseCaseFacade(port);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().name("Messages")
                .organizationId("123e4567-e89b-12d3-a456-426614174000").languageId("lang").stateId("state").build();

        facade.execute(dto);

        assertThat(port).isInstanceOf(CreateApplicationUseCase.class);
        verify(applicationValidator).validate(dto);
        verify(applicationRepository).createWithEnvironments(any(), any(), any(), any(), any());
        assertThat(Arrays.stream(HandlingCreateApplicationPort.class.getMethods())
                .map(Method::getName)).containsExactly("createApplication");
    }

    @Test
    void functionalityBeansRegisterUseCaseAndFacade() {
        when(loggerFactory.getLogger(CreateFunctionalityUseCase.class)).thenReturn(mock(LoggingPort.class));
        HandlingCreateFunctionalityPort port = config.handlingCreateFunctionalityPort(
                functionalityRepository, functionalityValidator, loggerFactory);
        CreateFunctionalityUseCaseFacade facade = config.createFunctionalityUseCaseFacade(port);
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().name("Search")
                .applicationId("123e4567-e89b-12d3-a456-426614175000").stateId("state").build();

        facade.execute(dto);

        assertThat(port).isInstanceOf(CreateFunctionalityUseCase.class);
        verify(functionalityValidator).validate(dto);
        verify(functionalityRepository).create(any(), any());
    }

    @Test
    void catalogUseCaseBeanReceivesSessionAccessValidator() {
        SessionApplicationAccessCompositeValidator accessValidator =
                config.sessionApplicationAccessCompositeValidator(catalog, applicationRepository);
        FindCatalogUseCaseFacade facade = config.findCatalogUseCaseFacade(config.handlingFindCatalogPort(
                applicationCatalogRepository, environmentCatalogRepository, functionalityCatalogRepository,
                messageTypeCatalogRepository, messageCategoryCatalogRepository, messageStateCatalogRepository,
                messageEnvironmentStateCatalogRepository, accessValidator));

        assertThat(facade).isNotNull();
        assertThat(config.handlingFindCatalogPort(applicationCatalogRepository, environmentCatalogRepository,
                functionalityCatalogRepository, messageTypeCatalogRepository, messageCategoryCatalogRepository,
                messageStateCatalogRepository, messageEnvironmentStateCatalogRepository, accessValidator))
                .isInstanceOf(FindCatalogUseCase.class);
    }

    @Test
    void messageEnvironmentResolverUsesSelectionValidatorAndCatalog() {
        MessageEnvironmentSelectionCompositeValidator environmentValidator =
                config.messageEnvironmentSelectionCompositeValidator(catalog, environmentRepository);

        MessageEnvironmentResolver resolver = config.messageEnvironmentResolver(environmentValidator, catalog);

        assertThat(resolver).isInstanceOf(MessageEnvironmentResolverImpl.class);
    }

    @Test
    void organizationValidatorRejectsBlankNameAndUsesCatalogMessage() {
        when(catalog.getMessage("FUN_147")).thenReturn("Organization name is required");
        CreateOrganizationCompositeValidator validator =
                config.createOrganizationCompositeValidator(catalog, organizationRepository);

        assertThatThrownBy(() -> validator.validate(CreateOrganizationDTO.builder().name(" ").build()))
                .isInstanceOf(co.edu.uco.crosscutting.exceptions.BusinessRuleException.class)
                .satisfies(error -> assertThat(((co.edu.uco.crosscutting.exceptions.BusinessRuleException) error)
                        .getUserMessage()).isEqualTo("Organization name is required"));
        verify(organizationRepository, never()).findByName(any());
    }

    @Test
    void organizationUseCaseAndFacadeAreWiredToRepositoryAndValidator() {
        when(loggerFactory.getLogger(CreateOrganizationUseCase.class)).thenReturn(mock(LoggingPort.class));
        CreateOrganizationCompositeValidator validator = mock(CreateOrganizationCompositeValidator.class);
        var port = config.handlingCreateOrganizationPort(organizationRepository, validator, loggerFactory);
        CreateOrganizationUseCaseFacade facade = config.createOrganizationUseCaseFacade(port);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();

        facade.execute(dto);

        verify(validator).validate(dto);
        org.mockito.ArgumentCaptor<co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity> captor =
                org.mockito.ArgumentCaptor.forClass(
                        co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity.class);
        verify(organizationRepository).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("UCO");
    }

    @Test
    void createMessageValidatorUsesConfiguredRulesAndRejectsNullRequest() {
        when(catalog.getMessage("FUN_010")).thenReturn("Invalid message data");
        CreateMessageCompositeValidator validator = config.createMessageCompositeValidator(catalog,
                recordExistsCatalogPort, functionalityCatalogRepository, messageCodeQueryPort,
                config.messageEnvironmentSelectionCompositeValidator(catalog, environmentRepository));

        assertThatThrownBy(() -> validator.validate((CreateMessageDTO) null, "language-id"))
                .isInstanceOf(co.edu.uco.crosscutting.exceptions.BusinessRuleException.class)
                .satisfies(error -> assertThat(((co.edu.uco.crosscutting.exceptions.BusinessRuleException) error)
                        .getUserMessage()).isEqualTo("Invalid message data"));
        verifyNoInteractions(recordExistsCatalogPort, functionalityCatalogRepository, messageCodeQueryPort);
    }

    @Test
    void createApplicationValidatorUsesConfiguredRulesAndCatalogMessage() {
        when(catalog.getMessage("FUN_161")).thenReturn("Application name required");
        var validator = config.createApplicationCompositeValidator(catalog, recordExistsCatalogPort,
                applicationRepository, organizationRepository);

        assertThatThrownBy(() -> validator.validate(CreateApplicationDTO.builder().name(" ").build()))
                .isInstanceOf(co.edu.uco.crosscutting.exceptions.BusinessRuleException.class)
                .satisfies(error -> assertThat(((co.edu.uco.crosscutting.exceptions.BusinessRuleException) error)
                        .getUserMessage()).isEqualTo("Application name required"));
        verifyNoInteractions(recordExistsCatalogPort, organizationRepository);
    }
}
