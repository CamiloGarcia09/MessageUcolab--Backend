package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.usecase.domain.security.AuthorizationScopeType;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingActiveContextPort;
import co.edu.uco.application.usecase.validator.authorization.AuthorizationCompositeValidator;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateApplicationUseCaseTest {

    private static final String ORGANIZATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String OTHER_ORGANIZATION_ID = "123e4567-e89b-12d3-a456-426614174002";
    private static final UUID ACTIVE_STATE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174003");
    private static final List<String> EXPECTED_ENVIRONMENT_TYPES = List.of("Develop", "Testing", "Production");
    private static final ExternalIdentity IDENTITY = new ExternalIdentity(
            "issuer", "subject", "user@example.com", Instant.MAX);

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository;
    @Mock
    private CreateApplicationCompositeValidator validator;
    @Mock
    private HandlingActiveContextPort activeContextPort;
    @Mock
    private AuthorizationCompositeValidator authorizationCompositeValidator;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Captor
    private ArgumentCaptor<ApplicationData> applicationCaptor;
    @Captor
    private ArgumentCaptor<List<EnvironmentData>> environmentsCaptor;

    private CreateApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CreateApplicationUseCase.class)).thenReturn(log);
        useCase = new CreateApplicationUseCase(applicationRepository, environmentReferenceCatalogRepository,
                validator, activeContextPort, authorizationCompositeValidator, catalogPort, loggerFactory);
    }

    private CreateApplicationDTO validDto() {
        return CreateApplicationDTO.builder()
                .name("Message App")
                .organizationId(ORGANIZATION_ID)
                .languageId("lang-1")
                .stateId("state-1")
                .build();
    }

    private List<EnvironmentTypeData> catalogWith(final String... names) {
        return Arrays.stream(names)
                .map(name -> new EnvironmentTypeData(UUID.randomUUID(), name))
                .collect(Collectors.toList());
    }

    private void stubResolvableCatalog() {
        when(environmentReferenceCatalogRepository.findAllTypes())
                .thenReturn(catalogWith("Develop", "Testing", "Production"));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE))
                .thenReturn(Optional.of(ACTIVE_STATE_ID));
    }

    private void verifyApplicationCreatedWithDefaultEnvironments() {
        verify(applicationRepository).createWithEnvironments(applicationCaptor.capture(), eq("lang-1"),
                eq("state-1"), environmentsCaptor.capture(), eq(ACTIVE_STATE_ID.toString()));
    }

    @Nested
    @DisplayName("Happy path: la aplicación crea sus tres ambientes por defecto")
    class DefaultEnvironmentsCreation {

        @Test
        @DisplayName("Crea exactamente tres ambientes Develop/Testing/Production en estado Active")
        void createApplication_persistsExactlyThreeDefaultEnvironmentsWithActiveState() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            List<EnvironmentData> environments = environmentsCaptor.getValue();
            ApplicationData application = applicationCaptor.getValue();
            assertSoftly(softly -> {
                softly.assertThat(environments).hasSize(EXPECTED_ENVIRONMENT_TYPES.size());
                softly.assertThat(environments)
                        .extracting(environment -> environment.getType().getName())
                        .containsExactlyElementsOf(EXPECTED_ENVIRONMENT_TYPES);
                softly.assertThat(application.getName()).isEqualTo("Message App");
                softly.assertThat(application.getOrganization().getId())
                        .isEqualTo(UUID.fromString(ORGANIZATION_ID));
            });
            verify(environmentReferenceCatalogRepository).findStateIdByName(STATE_ACTIVE);
            verify(validator).validate(dto);
            verify(log).info("Application created successfully with name: {}", "Message App");
        }

        @Test
        @DisplayName("Asigna UUIDs de ambiente únicos entre sí y distintos de la aplicación y de los tipos")
        void createApplication_generatesEnvironmentUuidsDistinctFromApplicationAndTypes() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            Set<UUID> environmentIds = environmentsCaptor.getValue().stream()
                    .map(EnvironmentData::getId)
                    .collect(Collectors.toSet());
            Set<UUID> typeIds = environmentsCaptor.getValue().stream()
                    .map(environment -> environment.getType().getId())
                    .collect(Collectors.toSet());
            assertSoftly(softly -> {
                softly.assertThat(environmentIds).hasSize(3);
                softly.assertThat(environmentIds).doesNotContain(applicationCaptor.getValue().getId());
                softly.assertThat(environmentIds).doesNotContainAnyElementsOf(typeIds);
                softly.assertThat(typeIds).hasSize(3);
            });
        }

        @Test
        @DisplayName("Asocia cada ambiente a la aplicación creada")
        void createApplication_linksEveryEnvironmentToTheCreatedApplication() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            ApplicationData application = applicationCaptor.getValue();
            assertThat(environmentsCaptor.getValue())
                    .hasSize(3)
                    .allSatisfy(environment -> assertSoftly(softly -> {
                        softly.assertThat(environment.getApplication()).isSameAs(application);
                        softly.assertThat(environment.getApplication().getId()).isEqualTo(application.getId());
                        softly.assertThat(environment.getType()).isNotNull();
                    }));
        }

        @Test
        @DisplayName("Resuelve los tipos aunque el catálogo venga desordenado y en mayúsculas")
        void createApplication_resolvesTypesCaseInsensitively_whenCatalogIsUnordered() {
            CreateApplicationDTO dto = validDto();
            UUID developId = UUID.randomUUID();
            UUID testingId = UUID.randomUUID();
            UUID productionId = UUID.randomUUID();
            List<EnvironmentTypeData> shuffledCatalog = List.of(
                    new EnvironmentTypeData(productionId, "PRODUCTION"),
                    new EnvironmentTypeData(developId, "dEvElOp"),
                    new EnvironmentTypeData(testingId, "testing"));
            when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(shuffledCatalog);
            when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE))
                    .thenReturn(Optional.of(ACTIVE_STATE_ID));

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            List<EnvironmentData> environments = environmentsCaptor.getValue();
            assertSoftly(softly -> {
                softly.assertThat(environments).hasSize(3);
                softly.assertThat(environments.get(0).getType().getName()).isEqualToIgnoringCase("Develop");
                softly.assertThat(environments.get(0).getType().getId()).isEqualTo(developId);
                softly.assertThat(environments.get(1).getType().getName()).isEqualToIgnoringCase("Testing");
                softly.assertThat(environments.get(1).getType().getId()).isEqualTo(testingId);
                softly.assertThat(environments.get(2).getType().getName()).isEqualToIgnoringCase("Production");
                softly.assertThat(environments.get(2).getType().getId()).isEqualTo(productionId);
            });
        }

        @Test
        @DisplayName("Ignora entradas nulas del catálogo de tipos de ambiente")
        void createApplication_ignoresNullCatalogEntries() {
            CreateApplicationDTO dto = validDto();
            List<EnvironmentTypeData> catalogWithNullEntry = new ArrayList<>();
            catalogWithNullEntry.add(null);
            catalogWithNullEntry.addAll(catalogWith("Develop", "Testing", "Production"));
            when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(catalogWithNullEntry);
            when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE))
                    .thenReturn(Optional.of(ACTIVE_STATE_ID));

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            assertThat(environmentsCaptor.getValue())
                    .extracting(environment -> environment.getType().getName())
                    .containsExactlyElementsOf(EXPECTED_ENVIRONMENT_TYPES);
        }

        @Test
        @DisplayName("Autoriza con el contexto activo y el permiso APPLICATION_CREATE")
        void createApplication_authorizesMatchingActiveContextWithApplicationCreatePermission() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();
            when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(
                    ActiveContextDTO.builder().organizationId(ORGANIZATION_ID).build());

            useCase.createApplication(dto, IDENTITY);

            verify(authorizationCompositeValidator).validate(IDENTITY, PermissionCode.APPLICATION_CREATE,
                    AuthorizationScopeType.ORGANIZATION, UUID.fromString(ORGANIZATION_ID));
            verify(validator).validate(dto);
            verify(applicationRepository).createWithEnvironments(any(ApplicationData.class),
                    eq("lang-1"), eq("state-1"),
                    argThat((List<EnvironmentData> environments) -> environments.size() == 3),
                    eq(ACTIVE_STATE_ID.toString()));
            verifyNoInteractions(catalogPort);
        }
    }

    @Nested
    @DisplayName("Casos límite del catálogo de referencia de ambientes")
    class ReferenceCatalogBoundaries {

        @Test
        @DisplayName("Falla con FUN_175 y no persiste cuando el catálogo de tipos es null")
        void createApplication_throwsBusinessRule_whenTypeCatalogIsNull() {
            CreateApplicationDTO dto = validDto();
            when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(null);
            when(catalogPort.getMessage("FUN_175")).thenReturn("El tipo de entorno no existe");

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo("El tipo de entorno no existe"));
            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Falla con FUN_175 y no persiste cuando el catálogo de tipos está vacío")
        void createApplication_throwsBusinessRule_whenTypeCatalogIsEmpty() {
            CreateApplicationDTO dto = validDto();
            when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of());
            when(catalogPort.getMessage("FUN_175")).thenReturn("El tipo de entorno no existe");

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo("El tipo de entorno no existe"));
            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Falla con FUN_175 cuando falta alguno de los tres tipos requeridos")
        void createApplication_throwsBusinessRule_whenATypeIsMissing() {
            CreateApplicationDTO dto = validDto();
            when(environmentReferenceCatalogRepository.findAllTypes())
                    .thenReturn(catalogWith("Develop", "Production"));
            when(catalogPort.getMessage("FUN_175")).thenReturn("El tipo de entorno no existe");

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo("El tipo de entorno no existe"));
            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Falla con FUN_177 y no persiste cuando el estado Active no existe")
        void createApplication_throwsBusinessRule_whenActiveStateIsMissing() {
            CreateApplicationDTO dto = validDto();
            when(environmentReferenceCatalogRepository.findAllTypes())
                    .thenReturn(catalogWith("Develop", "Testing", "Production"));
            when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE))
                    .thenReturn(Optional.empty());
            when(catalogPort.getMessage("FUN_177")).thenReturn("El estado del entorno no existe");

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo("El estado del entorno no existe"));
            verifyNoInteractions(applicationRepository);
        }
    }

    @Nested
    @DisplayName("Autorización y errores")
    class AuthorizationAndFailures {

        @Test
        @DisplayName("Falla con Forbidden cuando no existe contexto activo")
        void createApplication_throwsForbidden_whenActiveContextDoesNotExist() {
            CreateApplicationDTO dto = validDto();
            when(catalogPort.getMessage("FUN_153")).thenReturn("Fuera del contexto activo");

            ForbiddenException thrown = catchThrowableOfType(
                    () -> useCase.createApplication(dto, IDENTITY), ForbiddenException.class);

            assertAll(
                    () -> assertThat(thrown).isNotNull(),
                    () -> assertThat(thrown.getUserMessage()).isEqualTo("Fuera del contexto activo"),
                    () -> assertThat(thrown.getHttpStatus()).isEqualTo(403));
            verifyNoInteractions(applicationRepository, authorizationCompositeValidator);
            verify(validator, never()).validate(any());
        }

        @Test
        @DisplayName("Falla con Forbidden cuando el contexto activo está en otra organización")
        void createApplication_throwsForbidden_whenActiveContextIsOutsideRequestedOrganization() {
            CreateApplicationDTO dto = validDto();
            when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(
                    ActiveContextDTO.builder().organizationId(OTHER_ORGANIZATION_ID).build());
            when(catalogPort.getMessage("FUN_153")).thenReturn("Fuera del contexto activo");

            ForbiddenException thrown = catchThrowableOfType(
                    () -> useCase.createApplication(dto, IDENTITY), ForbiddenException.class);

            assertAll(
                    () -> assertThat(thrown).isNotNull(),
                    () -> assertThat(thrown.getUserMessage()).isEqualTo("Fuera del contexto activo"),
                    () -> assertThat(thrown.getHttpStatus()).isEqualTo(403));
            verifyNoInteractions(applicationRepository, authorizationCompositeValidator, validator);
        }

        @Test
        @DisplayName("Propaga el Forbidden devuelto por el validador de autorización")
        void createApplication_propagatesForbiddenFromAuthorizationCompositeValidator() {
            CreateApplicationDTO dto = validDto();
            ForbiddenException failure = ForbiddenException.buildUserException("Permission denied");
            when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(
                    ActiveContextDTO.builder().organizationId(ORGANIZATION_ID).build());
            doThrow(failure).when(authorizationCompositeValidator).validate(IDENTITY,
                    PermissionCode.APPLICATION_CREATE, AuthorizationScopeType.ORGANIZATION,
                    UUID.fromString(ORGANIZATION_ID));

            assertThatThrownBy(() -> useCase.createApplication(dto, IDENTITY)).isSameAs(failure);
            verifyNoInteractions(applicationRepository, validator);
        }

        @Test
        @DisplayName("Propaga el error de validación sin tocar repositorios")
        void createApplication_propagatesValidationError() {
            CreateApplicationDTO dto = validDto();
            doThrow(BusinessRuleException.buildUserException("Invalid application"))
                    .when(validator).validate(dto);

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo("Invalid application"));
            verifyNoInteractions(applicationRepository, environmentReferenceCatalogRepository);
        }

        @Test
        @DisplayName("Convierte un fallo inesperado del repositorio en BusinessException técnica")
        void createApplication_throwsBusinessException_whenRepositoryFails() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();
            doThrow(new RuntimeException("db down")).when(applicationRepository)
                    .createWithEnvironments(any(), anyString(), anyString(), any(), anyString());

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getTechnicalMessage())
                            .isEqualTo("Error al crear la aplicación"));
            verify(log).error(eq("Error creating application in repository"), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Relanza la excepción de dominio devuelta por el repositorio")
        void createApplication_rethrowsCrossWordsExceptionFromRepository() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();
            doThrow(BusinessRuleException.buildUserException("conflict")).when(applicationRepository)
                    .createWithEnvironments(any(), anyString(), anyString(), any(), anyString());

            assertThatThrownBy(() -> useCase.createApplication(dto, null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("Id explícito: en modo sesión la aplicación conserva el UUID del componente de seguridad")
    class ExplicitApplicationId {

        @Test
        @DisplayName("Usa el id recibido en lugar de generar uno nuevo")
        void createApplication_usesProvidedId_whenDtoHasId() {
            String securityApplicationId = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
            CreateApplicationDTO dto = validDto();
            dto.setId(securityApplicationId);
            stubResolvableCatalog();

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            assertThat(applicationCaptor.getValue().getId()).isEqualTo(UUID.fromString(securityApplicationId));
            assertThat(environmentsCaptor.getValue())
                    .allSatisfy(environment -> assertThat(environment.getApplication().getId())
                            .isEqualTo(UUID.fromString(securityApplicationId)));
        }

        @Test
        @DisplayName("Genera un id nuevo cuando no se envía")
        void createApplication_generatesId_whenDtoHasNoId() {
            CreateApplicationDTO dto = validDto();
            stubResolvableCatalog();

            useCase.createApplication(dto, null);

            verifyApplicationCreatedWithDefaultEnvironments();
            assertThat(applicationCaptor.getValue().getId()).isNotNull();
        }
    }
}
