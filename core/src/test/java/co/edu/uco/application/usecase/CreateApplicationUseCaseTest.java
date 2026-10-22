package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateApplicationUseCaseTest {
    private static final String APP_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final UUID ACTIVE_STATE_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174003");

    @Mock private ApplicationRepository applicationRepository;
    @Mock private EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository;
    @Mock private CreateApplicationCompositeValidator validator;
    @Mock private CatalogPort catalog;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort log;

    private CreateApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CreateApplicationUseCase.class)).thenReturn(log);
        useCase = new CreateApplicationUseCase(applicationRepository, environmentReferenceCatalogRepository,
                validator, catalog, loggerFactory);
    }

    @Test
    void createApplication_persistsApplicationWithThreeDefaultEnvironments() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().id(APP_ID).name("Message App")
                .organizationId("123e4567-e89b-12d3-a456-426614174000").languageId("lang-1").stateId("state-1").build();
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of(
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)).thenReturn(Optional.of(ACTIVE_STATE_ID));
        ArgumentCaptor<ApplicationData> appCaptor = ArgumentCaptor.forClass(ApplicationData.class);
        ArgumentCaptor<List<EnvironmentData>> envCaptor = ArgumentCaptor.forClass(List.class);

        useCase.createApplication(dto);

        verify(applicationRepository).createWithEnvironments(appCaptor.capture(), eq("lang-1"), eq("state-1"),
                envCaptor.capture(), eq(ACTIVE_STATE_ID.toString()));
        assertThat(appCaptor.getValue().getId()).isEqualTo(UUID.fromString(APP_ID));
        assertThat(envCaptor.getValue()).extracting(environment -> environment.getType().getName())
                .containsExactly("Develop", "Testing", "Production");
        verify(validator).validate(dto);
    }

    @Test
    void createApplication_rejectsMissingEnvironmentTypeWithoutPersisting() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().name("App")
                .organizationId("123e4567-e89b-12d3-a456-426614174000")
                .languageId("lang").stateId("state").build();
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of());
        when(catalog.getMessage("FUN_175")).thenReturn("Environment type missing");

        assertThatThrownBy(() -> useCase.createApplication(dto)).isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getUserMessage())
                        .isEqualTo("Environment type missing"));

        verify(applicationRepository, never()).createWithEnvironments(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void createApplication_wrapsUnexpectedRepositoryFailureAsBusinessException() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().name("App")
                .organizationId("123e4567-e89b-12d3-a456-426614174000")
                .languageId("lang").stateId("state").build();
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(List.of(
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)).thenReturn(Optional.of(ACTIVE_STATE_ID));
        doThrow(new IllegalStateException("repository unavailable")).when(applicationRepository)
                .createWithEnvironments(any(), anyString(), anyString(), any(), anyString());

        assertThatThrownBy(() -> useCase.createApplication(dto)).isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getTechnicalMessage())
                        .isEqualTo("Error al crear la aplicación"));
    }

    @Test
    void createApplication_createsDistinctEnvironmentIdsLinkedToApplication() {
        stubReferenceCatalog(List.of(new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        CreateApplicationDTO dto = validDto();
        ArgumentCaptor<ApplicationData> appCaptor = ArgumentCaptor.forClass(ApplicationData.class);
        ArgumentCaptor<List<EnvironmentData>> environmentsCaptor = ArgumentCaptor.forClass(List.class);

        useCase.createApplication(dto);

        verify(applicationRepository).createWithEnvironments(appCaptor.capture(), anyString(), anyString(),
                environmentsCaptor.capture(), anyString());
        List<EnvironmentData> environments = environmentsCaptor.getValue();
        assertThat(environments).extracting(EnvironmentData::getId).doesNotHaveDuplicates()
                .doesNotContain(appCaptor.getValue().getId());
        assertThat(environments).allSatisfy(environment -> assertThat(environment.getApplication())
                .isSameAs(appCaptor.getValue()));
    }

    @Test
    void createApplication_resolvesUnorderedCaseInsensitiveCatalogAndIgnoresNullEntries() {
        UUID developId = UUID.randomUUID();
        UUID testingId = UUID.randomUUID();
        UUID productionId = UUID.randomUUID();
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(java.util.Arrays.asList(null,
                new EnvironmentTypeData(productionId, "PRODUCTION"),
                new EnvironmentTypeData(developId, "dEvElOp"), new EnvironmentTypeData(testingId, "testing")));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)).thenReturn(Optional.of(ACTIVE_STATE_ID));
        ArgumentCaptor<List<EnvironmentData>> captor = ArgumentCaptor.forClass(List.class);

        useCase.createApplication(validDto());

        verify(applicationRepository).createWithEnvironments(any(), anyString(), anyString(), captor.capture(), anyString());
        assertThat(captor.getValue()).extracting(environment -> environment.getType().getId())
                .containsExactly(developId, testingId, productionId);
    }

    @Test
    void createApplication_rejectsNullTypeCatalogWithoutWriting() {
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(null);
        when(catalog.getMessage("FUN_175")).thenReturn("Missing environment type");

        assertThatThrownBy(() -> useCase.createApplication(validDto())).isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getUserMessage())
                        .isEqualTo("Missing environment type"));
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void createApplication_rejectsMissingActiveStateWithoutWriting() {
        stubReferenceCatalog(List.of(new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)).thenReturn(Optional.empty());
        when(catalog.getMessage("FUN_177")).thenReturn("Active state missing");

        assertThatThrownBy(() -> useCase.createApplication(validDto())).isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getUserMessage())
                        .isEqualTo("Active state missing"));
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void createApplication_propagatesValidatorFailureBeforeCatalogOrRepository() {
        CreateApplicationDTO dto = validDto();
        BusinessRuleException failure = BusinessRuleException.buildUserException("Invalid application");
        doThrow(failure).when(validator).validate(dto);

        assertThatThrownBy(() -> useCase.createApplication(dto)).isSameAs(failure);

        verifyNoInteractions(applicationRepository, environmentReferenceCatalogRepository);
    }

    @Test
    void createApplication_propagatesDomainRepositoryFailure() {
        stubReferenceCatalog(List.of(new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        BusinessRuleException failure = BusinessRuleException.buildUserException("Domain rejection");
        doThrow(failure).when(applicationRepository).createWithEnvironments(any(), anyString(), anyString(),
                any(), anyString());

        assertThatThrownBy(() -> useCase.createApplication(validDto())).isSameAs(failure);
    }

    @Test
    void createApplication_generatesIdentifierWhenDtoDoesNotProvideOne() {
        stubReferenceCatalog(List.of(new EnvironmentTypeData(UUID.randomUUID(), "Develop"),
                new EnvironmentTypeData(UUID.randomUUID(), "Testing"),
                new EnvironmentTypeData(UUID.randomUUID(), "Production")));
        ArgumentCaptor<ApplicationData> captor = ArgumentCaptor.forClass(ApplicationData.class);

        useCase.createApplication(validDto());

        verify(applicationRepository).createWithEnvironments(captor.capture(), anyString(), anyString(), any(), anyString());
        assertThat(captor.getValue().getId()).isNotNull();
    }

    private CreateApplicationDTO validDto() {
        return CreateApplicationDTO.builder().name("Message App")
                .organizationId("123e4567-e89b-12d3-a456-426614174000")
                .languageId("lang-1").stateId("state-1").build();
    }

    private void stubReferenceCatalog(List<EnvironmentTypeData> types) {
        when(environmentReferenceCatalogRepository.findAllTypes()).thenReturn(types);
        when(environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)).thenReturn(Optional.of(ACTIVE_STATE_ID));
    }
}
