package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.context.SelectActiveContextDTO;
import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.facade.application.CreateApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.primaryports.facade.organization.CreateOrganizationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.cache.ActiveContextCachePort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.secondaryports.repository.ActiveContextRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.ExternalIdentityRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.secondaryports.security.AuthorizationQueryPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.AuthorizationScopeType;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingCreateOrganizationPort;
import co.edu.uco.application.usecase.handling.HandlingActiveContextPort;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingFindCatalogPort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolverImpl;
import co.edu.uco.application.usecase.FindCatalogUseCase;
import co.edu.uco.application.usecase.validator.authorization.AuthorizationCompositeValidator;
import co.edu.uco.application.usecase.validator.authorization.rule.ExternalIdentityRequiredRule;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.context.SelectActiveContextCompositeValidator;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.application.usecase.validator.organization.CreateOrganizationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.lang.reflect.Method;
import java.util.Arrays;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UseCaseRuleConfigTest {

    private static final String LANGUAGE_ID = "123e4567-e89b-12d3-a456-426614175601";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175602";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private CreateOrganizationCompositeValidator validator;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private HandlingCreateOrganizationPort handlingPort;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock
    private CreateApplicationCompositeValidator applicationValidator;
    @Mock
    private HandlingCreateApplicationPort handlingApplicationPort;
    @Mock
    private AuthorizationQueryPort authorizationQueryPort;
    @Mock
    private AuthorizationCompositeValidator authorizationCompositeValidator;
    @Mock
    private ApplicationCatalogRepository applicationCatalogRepository;
    @Mock
    private EnvironmentCatalogRepository environmentCatalogRepository;
    @Mock
    private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock
    private MessageCodeQueryPort messageCodeQueryPort;
    @Mock
    private MessageTypeCatalogRepository messageTypeCatalogRepository;
    @Mock
    private MessageCategoryCatalogRepository messageCategoryCatalogRepository;
    @Mock
    private MessageStateCatalogRepository messageStateCatalogRepository;
    @Mock
    private MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository;
    @Mock
    private HandlingFindCatalogPort handlingFindCatalogPort;
    @Mock
    private ActiveContextRepository activeContextRepository;
    @Mock
    private ActiveContextCachePort activeContextCachePort;
    @Mock
    private ExternalIdentityRepository externalIdentityRepository;
    @Mock
    private EnvironmentRepository environmentRepository;
    @Mock
    private EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository;
    @Mock
    private ExternalIdentityRequiredRule externalIdentityRequiredRule;
    @Mock
    private SelectActiveContextCompositeValidator activeContextValidator;
    @Mock
    private HandlingActiveContextPort handlingActiveContextPort;
    @Mock
    private Clock clock;

    private UseCaseRuleConfig config;

    @BeforeEach
    void setUp() {
        config = new UseCaseRuleConfig();
    }

    @Test
    void createOrganizationCompositeValidator_rejectsBlankNameUsingCatalogMessage() {
        when(catalogPort.getMessage("FUN_147")).thenReturn("Nombre requerido");
        CreateOrganizationCompositeValidator composite =
                config.createOrganizationCompositeValidator(catalogPort, organizationRepository);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("").build();

        assertThatThrownBy(() -> composite.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("userMessage")
                .isEqualTo("Nombre requerido");
        verify(catalogPort).getMessage("FUN_147");
    }

    @Test
    void createOrganizationCompositeValidator_rejectsDuplicatedNameThroughRepository() {
        when(catalogPort.getMessage("FUN_149")).thenReturn("Nombre duplicado");
        when(organizationRepository.findByName("UCO")).thenReturn(Optional.of(new OrganizationEntity()));
        CreateOrganizationCompositeValidator composite =
                config.createOrganizationCompositeValidator(catalogPort, organizationRepository);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();

        assertThatThrownBy(() -> composite.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("userMessage")
                .isEqualTo("Nombre duplicado");
        verify(organizationRepository).findByName("UCO");
    }

    @Test
    void handlingCreateOrganizationPort_connectsValidatorRepositoryAndLogger() {
        when(loggerFactory.getLogger(any())).thenReturn(log);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        HandlingCreateOrganizationPort port = config.handlingCreateOrganizationPort(
                organizationRepository, validator, loggerFactory);

        port.createOrganization(dto);

        verify(validator).validate(dto);
        verify(organizationRepository).create(any(OrganizationEntity.class));
        verify(log).info("Organization created successfully");
    }

    @Test
    void createOrganizationUseCaseFacade_connectsHandlingPort() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        CreateOrganizationUseCaseFacade facade = config.createOrganizationUseCaseFacade(handlingPort);

        facade.execute(dto);

        verify(handlingPort).createOrganization(dto);
    }

    @Test
    void createApplicationCompositeValidator_connectsOrganizationAndExistingRules() {
        UUID organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(new OrganizationEntity()));
        when(recordExistsCatalogPort.exists(any(), any())).thenReturn(true);
        when(applicationRepository.findByName("Messages")).thenReturn(Optional.empty());
        CreateApplicationCompositeValidator composite = config.createApplicationCompositeValidator(
                catalogPort, recordExistsCatalogPort, applicationRepository,
                organizationRepository);
        CreateApplicationDTO dto = validApplicationDto();

        composite.validate(dto);

        verify(organizationRepository).findById(organizationId);
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID);
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.APPLICATION_STATE, STATE_ID);
        verify(applicationRepository).findByName("Messages");
    }

    @Test
    void handlingCreateApplicationPort_connectsValidatorReferenceCatalogAndLogger() {
        when(loggerFactory.getLogger(any())).thenReturn(log);
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of(
                new EnvironmentTypeData(UUID.fromString("423e4567-e89b-12d3-a456-426614174000"), "Develop"),
                new EnvironmentTypeData(UUID.fromString("423e4567-e89b-12d3-a456-426614174001"), "Testing"),
                new EnvironmentTypeData(UUID.fromString("423e4567-e89b-12d3-a456-426614174002"), "Production")));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE))
                .thenReturn(Optional.of(UUID.fromString("523e4567-e89b-12d3-a456-426614174000")));
        CreateApplicationDTO dto = validApplicationDto();
        HandlingCreateApplicationPort port = config.handlingCreateApplicationPort(
                applicationRepository, environmentReferenceCatalogRepository, applicationValidator,
                handlingActiveContextPort, authorizationCompositeValidator, catalogPort, loggerFactory);

        port.createApplication(dto, null);

        verify(applicationValidator).validate(dto);
        verify(applicationRepository).createWithEnvironments(any(ApplicationData.class),
                eq(LANGUAGE_ID), eq(STATE_ID),
                argThat((List<EnvironmentData> environments) -> environments.size() == 3
                        && environments.stream()
                        .map(environment -> environment.getType().getName())
                        .toList()
                        .equals(List.of("Develop", "Testing", "Production"))),
                eq("523e4567-e89b-12d3-a456-426614174000"));
        verify(log).info("Application created successfully with name: {}", "Messages");
        verifyNoInteractions(handlingActiveContextPort, authorizationCompositeValidator);
    }

    @Test
    void useCaseRuleConfig_declaresNoBeanForTheRemovedManualEnvironmentCreationFlow() {
        List<String> manualEnvironmentCreationBeans = Arrays.stream(UseCaseRuleConfig.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).contains("environment"))
                .filter(name -> name.toLowerCase(Locale.ROOT).contains("create"))
                .toList();

        assertThat(manualEnvironmentCreationBeans).isEmpty();
    }

    @Test
    void createApplicationUseCaseFacade_connectsHandlingPort() {
        CreateApplicationDTO dto = validApplicationDto();
        CreateApplicationUseCaseFacade facade = config.createApplicationUseCaseFacade(handlingApplicationPort);

        facade.execute(dto, null);

        verify(handlingApplicationPort).createApplication(dto, null);
    }

    @Test
    void authorizationCompositeValidator_delegatesToPermissionRule_whenPermissionIsGranted() {
        ExternalIdentity identity = identity();
        AuthorizationCompositeValidator composite =
                config.authorizationCompositeValidator(authorizationQueryPort, catalogPort);
        when(authorizationQueryPort.hasPermission(identity, PermissionCode.CONTEXT_SELECT,
                AuthorizationScopeType.APPLICATION, null)).thenReturn(true);

        assertThat(composite).isExactlyInstanceOf(AuthorizationCompositeValidator.class);
        assertThatCode(() -> composite.validate(identity, PermissionCode.CONTEXT_SELECT,
                AuthorizationScopeType.APPLICATION, null)).doesNotThrowAnyException();
        verify(authorizationQueryPort).hasPermission(identity, PermissionCode.CONTEXT_SELECT,
                AuthorizationScopeType.APPLICATION, null);
    }

    @Test
    void authorizationCompositeValidator_throwsUnauthorizedUsingFun152_whenExternalIdentityIsNull() {
        when(catalogPort.getMessage("FUN_152")).thenReturn("Autenticacion requerida");
        AuthorizationCompositeValidator composite =
                config.authorizationCompositeValidator(authorizationQueryPort, catalogPort);

        assertThatThrownBy(() -> composite.validate(null, PermissionCode.CONTEXT_SELECT,
                AuthorizationScopeType.APPLICATION, null))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("userMessage")
                .isEqualTo("Autenticacion requerida");

        verifyNoInteractions(authorizationQueryPort);
    }

    @Test
    void externalIdentityRequiredRule_usesAuthenticationCatalogMessage() {
        when(catalogPort.getMessage("FUN_152")).thenReturn("Authentication required");
        ExternalIdentityRequiredRule rule = config.externalIdentityRequiredRule(catalogPort);

        assertThat(rule).isExactlyInstanceOf(ExternalIdentityRequiredRule.class);
        assertThatThrownBy(() -> rule.validate(null))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("userMessage")
                .isEqualTo("Authentication required");
    }

    @Test
    void selectActiveContextCompositeValidator_rejectsMissingIdentifiersUsingCatalogMessage() {
        when(catalogPort.getMessage("FUN_155")).thenReturn("Identificadores requeridos");
        SelectActiveContextCompositeValidator composite = config.selectActiveContextCompositeValidator(
                catalogPort, organizationRepository, applicationRepository, environmentRepository);
        SelectActiveContextDTO context = new SelectActiveContextDTO();

        assertThatThrownBy(() -> composite.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("userMessage")
                .isEqualTo("Identificadores requeridos");

        verifyNoInteractions(organizationRepository, applicationRepository, environmentRepository);
    }

    @Test
    void selectActiveContextCompositeValidator_delegatesToHierarchyRepositories() {
        UUID organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID applicationId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(organizationId);
        ApplicationData application = ApplicationData.build(applicationId, "App", organization);
        co.edu.uco.application.secondaryports.entity.EnvironmentData environment =
                new co.edu.uco.application.secondaryports.entity.EnvironmentData(
                        UUID.fromString("323e4567-e89b-12d3-a456-426614174000"), application,
                        new co.edu.uco.application.secondaryports.entity.EnvironmentTypeData(
                                UUID.fromString("423e4567-e89b-12d3-a456-426614174000"), "Dev"));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(applicationRepository.findById(applicationId.toString())).thenReturn(Optional.of(application));
        when(environmentRepository.findById(environment.getId().toString())).thenReturn(Optional.of(environment));
        SelectActiveContextCompositeValidator composite = config.selectActiveContextCompositeValidator(
                catalogPort, organizationRepository, applicationRepository, environmentRepository);

        assertThatCode(() -> composite.validate(contextSelection())).doesNotThrowAnyException();

        verify(organizationRepository).findById(organizationId);
        verify(applicationRepository, atLeastOnce()).findById(applicationId.toString());
        verify(environmentRepository, atLeastOnce()).findById(environment.getId().toString());
    }

    @Test
    void handlingActiveContextPort_wiresAllUseCaseDependencies() {
        HandlingActiveContextPort port = config.handlingActiveContextPort(
                activeContextRepository, activeContextCachePort, externalIdentityRepository,
                applicationCatalogRepository, environmentCatalogRepository, authorizationQueryPort,
                authorizationCompositeValidator, externalIdentityRequiredRule, activeContextValidator, catalogPort, clock);

        assertThat(port).isInstanceOf(co.edu.uco.application.usecase.ActiveContextUseCase.class);
    }

    @Test
    void activeContextUseCaseFacade_connectsHandlingPort() {
        ActiveContextUseCaseFacade facade = config.activeContextUseCaseFacade(handlingActiveContextPort);

        facade.findAvailableContexts(null);

        verify(handlingActiveContextPort).findAvailableContexts(null);
    }

    @Test
    void messageEnvironmentResolver_connectsActiveContextAndAuthorization() {
        MessageEnvironmentResolver resolver = config.messageEnvironmentResolver(
                handlingActiveContextPort, authorizationCompositeValidator, environmentRepository, catalogPort);

        assertThat(resolver).isInstanceOf(MessageEnvironmentResolverImpl.class);
    }

    @Test
    void handlingFindCatalogPort_wiresCatalogRepositoriesAndAuthorization() {
        HandlingFindCatalogPort port = config.handlingFindCatalogPort(
                applicationCatalogRepository,
                environmentCatalogRepository,
                functionalityCatalogRepository,
                messageTypeCatalogRepository,
                messageCategoryCatalogRepository,
                messageStateCatalogRepository,
                messageEnvironmentStateCatalogRepository,
                authorizationQueryPort,
                authorizationCompositeValidator,
                handlingActiveContextPort,
                catalogPort);

        assertThat(port).isInstanceOf(FindCatalogUseCase.class);
    }

    @Test
    void findCatalogUseCaseFacade_connectsHandlingPort() {
        FindCatalogUseCaseFacade facade = config.findCatalogUseCaseFacade(handlingFindCatalogPort);

        facade.findApplications(null);

        verify(handlingFindCatalogPort).findApplications(null);
    }

    @Test
    void createMessageCompositeValidator_rejectsNullDtoUsingCatalogMessage() {
        when(catalogPort.getMessage("FUN_010")).thenReturn("Datos invalidos");
        CreateMessageCompositeValidator composite = config.createMessageCompositeValidator(
                catalogPort, recordExistsCatalogPort, functionalityCatalogRepository, messageCodeQueryPort,
                config.messageEnvironmentSelectionCompositeValidator(catalogPort, environmentRepository));

        assertThatThrownBy(() -> composite.validate((CreateMessageDTO) null, LANGUAGE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("userMessage")
                .isEqualTo("Datos invalidos");

        verifyNoInteractions(recordExistsCatalogPort, functionalityCatalogRepository, messageCodeQueryPort);
    }

    private CreateApplicationDTO validApplicationDto() {
        return CreateApplicationDTO.builder()
                .name("Messages")
                .organizationId("123e4567-e89b-12d3-a456-426614174000")
                .languageId(LANGUAGE_ID)
                .stateId(STATE_ID)
                .build();
    }

    private SelectActiveContextDTO contextSelection() {
        return SelectActiveContextDTO.builder()
                .organizationId("123e4567-e89b-12d3-a456-426614174000")
                .applicationId("223e4567-e89b-12d3-a456-426614174000")
                .environmentId("323e4567-e89b-12d3-a456-426614174000")
                .build();
    }

    private static ExternalIdentity identity() {
        return new ExternalIdentity("issuer", "subject", null, Instant.MAX);
    }
}
