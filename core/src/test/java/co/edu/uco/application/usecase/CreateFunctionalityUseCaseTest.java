package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.usecase.validator.functionality.CreateFunctionalityCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFunctionalityUseCaseTest {
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614175000";

    @Mock private FunctionalityRepository repository;
    @Mock private CreateFunctionalityCompositeValidator validator;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort log;
    @Mock private CatalogPort catalogPort;

    private CreateFunctionalityUseCase useCase;

    @BeforeEach
    void setUp() {
        CatalogPortStaticRef.set(null);
        lenient().when(catalogPort.getMessage("TCH_089")).thenReturn("Funcionalidad creada correctamente con nombre: {}");
        lenient().when(catalogPort.getMessage("TCH_090")).thenReturn("Ocurrió un error al crear la funcionalidad en el repositorio.");
        CatalogPortStaticRef.set(catalogPort);
        when(loggerFactory.getLogger(CreateFunctionalityUseCase.class)).thenReturn(log);
        useCase = new CreateFunctionalityUseCase(repository, validator, loggerFactory);
    }

    @AfterEach
    void resetCatalogReference() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void createFunctionality_validatesAndPersistsUnderDtoApplication() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().name("Search messages")
                .applicationId(APP_ID).stateId("state-1").build();
        ArgumentCaptor<FunctionalityData> captor = ArgumentCaptor.forClass(FunctionalityData.class);

        useCase.createFunctionality(dto);

        verify(validator).validate(dto);
        verify(repository).create(captor.capture(), eq("state-1"));
        assertThat(captor.getValue().getName()).isEqualTo("Search messages");
        assertThat(captor.getValue().getApplication().getId().toString()).isEqualTo(APP_ID);
        verify(log).info("Funcionalidad creada correctamente con nombre: {}", "Search messages");
    }

    @Test
    void createFunctionality_propagatesValidationFailureBeforeRepositoryAccess() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().name(" ").applicationId(APP_ID)
                .stateId("state-1").build();
        doThrow(BusinessRuleException.buildUserException("Invalid functionality")).when(validator).validate(dto);

        assertThatThrownBy(() -> useCase.createFunctionality(dto)).isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getUserMessage())
                        .isEqualTo("Invalid functionality"));

        verifyNoInteractions(repository);
    }

    @Test
    void createFunctionality_wrapsUnexpectedRepositoryFailureAsBusinessException() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().name("Search messages")
                .applicationId(APP_ID).stateId("state-1").build();
        doThrow(new IllegalStateException("repository unavailable")).when(repository).create(any(), anyString());

        assertThatThrownBy(() -> useCase.createFunctionality(dto)).isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getTechnicalMessage())
                        .isEqualTo("Ocurrió un error al crear la funcionalidad en el repositorio."));
    }

    @Test
    void createFunctionality_propagatesDomainRepositoryFailure() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().name("Search messages")
                .applicationId(APP_ID).stateId("state-1").build();
        BusinessRuleException failure = BusinessRuleException.buildUserException("Duplicate functionality");
        doThrow(failure).when(repository).create(any(), anyString());

        assertThatThrownBy(() -> useCase.createFunctionality(dto)).isSameAs(failure);
    }
}
