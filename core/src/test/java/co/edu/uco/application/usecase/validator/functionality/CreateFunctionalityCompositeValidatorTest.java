package co.edu.uco.application.usecase.validator.functionality;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFunctionalityCompositeValidatorTest {

    private static final String APP_UUID = "123e4567-e89b-12d3-a456-426614175000";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175401";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String INVALID_UUID = "not-a-uuid";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private FunctionalityRepository functionalityRepository;

    private CreateFunctionalityCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CreateFunctionalityCompositeValidator(catalogPort, recordExistsCatalogPort,
                applicationRepository, functionalityRepository);
    }

    private CreateFunctionalityDTO validDto() {
        return CreateFunctionalityDTO.builder()
                .name("Search messages")
                .applicationId(APP_UUID)
                .stateId(STATE_ID)
                .build();
    }

    @Test
    void validate_acceptsValidDto() {
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(functionalityRepository.existsByNameAndApplicationId("Search messages", APP_UUID)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(validDto()));
    }

    @Test
    void validate_checksExactFunctionalityStateCatalogAndUuidPairBeforeDuplicateCheck() {
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(ReferenceCatalog.FUNCTIONALITY_STATE, STATE_ID)).thenReturn(true);
        when(functionalityRepository.existsByNameAndApplicationId("Search messages", APP_UUID)).thenReturn(false);
        CreateFunctionalityDTO dto = validDto();

        validator.validate(dto);

        InOrder catalogOrder = inOrder(recordExistsCatalogPort, functionalityRepository);
        catalogOrder.verify(recordExistsCatalogPort).exists(ReferenceCatalog.FUNCTIONALITY_STATE, STATE_ID);
        catalogOrder.verify(functionalityRepository).existsByNameAndApplicationId("Search messages", APP_UUID);
    }

    @Test
    void validate_throwsBusinessRule_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_010.getCode())).thenReturn("Datos no validos");

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Datos no validos"));
    }

    @Test
    void validate_throwsBusinessRule_whenNameIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_179.getCode()))
                .thenReturn("El nombre de la funcionalidad es requerido.");

        assertThatThrownBy(() -> validator.validate(new CreateFunctionalityDTO()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El nombre de la funcionalidad es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenNameExceedsMaxLength() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_180.getCode()))
                .thenReturn("El nombre de la funcionalidad no puede superar los 50 caracteres.");
        CreateFunctionalityDTO dto = validDto();
        dto.setName("a".repeat(51));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El nombre de la funcionalidad no puede superar los 50 caracteres."));
    }

    @Test
    void validate_throwsBusinessRule_whenApplicationIdIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_172.getCode()))
                .thenReturn("El id de la aplicación es requerido.");
        CreateFunctionalityDTO dto = validDto();
        dto.setApplicationId("");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El id de la aplicación es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenApplicationDoesNotExist() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_181.getCode()))
                .thenReturn("La aplicación a la que se asocia la funcionalidad no existe.");
        when(applicationRepository.existsById(APP_UUID)).thenReturn(false);
        CreateFunctionalityDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("La aplicación a la que se asocia la funcionalidad no existe."));
    }

    @Test
    void validate_throwsBusinessRule_whenApplicationIdIsDefaultUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn("El uuid no es valido");
        CreateFunctionalityDTO dto = validDto();
        dto.setApplicationId(DEFAULT_UUID);

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El uuid no es valido"));
        verifyNoInteractions(applicationRepository, recordExistsCatalogPort, functionalityRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenStateIdIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_184.getCode()))
                .thenReturn("El estado de la funcionalidad es requerido.");
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        CreateFunctionalityDTO dto = validDto();
        dto.setStateId("");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la funcionalidad es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenStateDoesNotExist() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_185.getCode()))
                .thenReturn("El estado de la funcionalidad no existe.");
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(ReferenceCatalog.FUNCTIONALITY_STATE, STATE_ID)).thenReturn(false);
        CreateFunctionalityDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la funcionalidad no existe."));
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.FUNCTIONALITY_STATE, STATE_ID);
        verifyNoInteractions(functionalityRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenStateIdIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_185.getCode()))
                .thenReturn("El estado de la funcionalidad no existe.");
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        CreateFunctionalityDTO dto = validDto();
        dto.setStateId(INVALID_UUID);

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la funcionalidad no existe."));
        verifyNoInteractions(recordExistsCatalogPort, functionalityRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenNameAlreadyExistsForApplication() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_186.getCode()))
                .thenReturn("Ya existe una funcionalidad con el mismo nombre para la aplicación.");
        when(applicationRepository.existsById(APP_UUID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(functionalityRepository.existsByNameAndApplicationId("Search messages", APP_UUID)).thenReturn(true);
        CreateFunctionalityDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Ya existe una funcionalidad con el mismo nombre para la aplicación."));
    }
}