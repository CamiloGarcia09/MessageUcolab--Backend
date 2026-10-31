package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.validator.organization.CreateOrganizationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrganizationUseCaseTest {

    private static final UUID DEFAULT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private CreateOrganizationCompositeValidator validator;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private CreateOrganizationUseCase useCase;

    @BeforeEach
    void setUp() {
        CatalogPortStaticRef.set(null);
        lenient().when(catalogPort.getMessage("TCH_091")).thenReturn("Organización creada correctamente.");
        lenient().when(catalogPort.getMessage("TCH_092"))
                .thenReturn("Ocurrió un error al crear la organización en el repositorio.");
        CatalogPortStaticRef.set(catalogPort);
        when(loggerFactory.getLogger(CreateOrganizationUseCase.class)).thenReturn(log);
        useCase = new CreateOrganizationUseCase(organizationRepository, validator, loggerFactory);
    }

    @AfterEach
    void resetCatalogReference() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void createOrganization_validatesAndPersistsEntityWithGeneratedIdAndName() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        ArgumentCaptor<OrganizationEntity> entityCaptor = ArgumentCaptor.forClass(OrganizationEntity.class);
        ArgumentCaptor<String> logMessageCaptor = ArgumentCaptor.forClass(String.class);

        useCase.createOrganization(dto);

        verify(validator).validate(dto);
        verify(organizationRepository).create(entityCaptor.capture());
        assertThat(entityCaptor.getValue())
                .returns("UCO", OrganizationEntity::getName)
                .matches(entity -> entity.getId() != null && !DEFAULT_UUID.equals(entity.getId()),
                        "has a generated non-default identifier");
        verify(log).info(logMessageCaptor.capture());
        assertThat(logMessageCaptor.getValue())
                .isEqualTo("Organización creada correctamente.")
                .doesNotContain(dto.getName());
    }

    @Test
    void createOrganization_persistsNameAtFiftyCharacterLimit() {
        String boundaryName = "a".repeat(50);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(boundaryName).build();
        ArgumentCaptor<OrganizationEntity> entityCaptor = ArgumentCaptor.forClass(OrganizationEntity.class);

        useCase.createOrganization(dto);

        verify(organizationRepository).create(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getName()).isEqualTo(boundaryName);
    }

    @Test
    void createOrganization_doesNotPersistWhenValidationFails() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("").build();
        BusinessRuleException expected = BusinessRuleException.buildUserException("Nombre requerido");
        doThrow(expected).when(validator).validate(dto);

        assertThatThrownBy(() -> useCase.createOrganization(dto)).isSameAs(expected);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void createOrganization_rethrowsCrossWordsExceptionFromRepository() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        BusinessRuleException expected = BusinessRuleException.buildUserException("Nombre duplicado");
        doThrow(expected).when(organizationRepository).create(any(OrganizationEntity.class));

        assertThatThrownBy(() -> useCase.createOrganization(dto)).isSameAs(expected);
        verify(log, never()).error(any(String.class), any(Throwable.class));
    }

    @Test
    void createOrganization_wrapsRuntimeExceptionAsApplicationBusinessException() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        RuntimeException repositoryFailure = new RuntimeException("database unavailable");
        doThrow(repositoryFailure).when(organizationRepository).create(any(OrganizationEntity.class));

        assertThatThrownBy(() -> useCase.createOrganization(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat((BusinessException) exception)
                        .extracting(BusinessException::getTechnicalMessage,
                                BusinessException::getRootException,
                                BusinessException::getLocation)
                        .containsExactly("Ocurrió un error al crear la organización en el repositorio.",
                                repositoryFailure,
                                ExceptionLocation.APPLICATION));
        verify(log).error("Ocurrió un error al crear la organización en el repositorio.", repositoryFailure);
        verify(log, never()).error(eq("UCO"), any(Throwable.class));
    }
}
