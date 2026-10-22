package co.edu.uco.application.usecase.validator.application;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateApplicationCompositeValidatorTest {

    private static final String ORGANIZATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String LANGUAGE_ID = "123e4567-e89b-12d3-a456-426614175301";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175302";
    private static final String INVALID_UUID = "not-a-uuid";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private CreateApplicationCompositeValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(organizationRepository.findById(UUID.fromString(ORGANIZATION_ID)))
                .thenReturn(Optional.of(new OrganizationEntity()));
        validator = new CreateApplicationCompositeValidator(
                catalogPort, recordExistsCatalogPort, applicationRepository, organizationRepository);
    }

    private CreateApplicationDTO validDto() {
        return CreateApplicationDTO.builder()
                .name("Message App")
                .organizationId(ORGANIZATION_ID)
                .languageId(LANGUAGE_ID)
                .stateId(STATE_ID)
                .build();
    }

    @Test
    void validate_acceptsValidDto() {
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(validDto()));
    }

    @Test
    void validate_looksUpOrganizationBeforeCatalogAndDuplicateChecks() {
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(ReferenceCatalog.APPLICATION_STATE, STATE_ID)).thenReturn(true);
        when(applicationRepository.existsByNameAndOrganization("Message App", ORGANIZATION_ID)).thenReturn(false);
        CreateApplicationDTO dto = validDto();

        validator.validate(dto);

        InOrder validationOrder = inOrder(organizationRepository, recordExistsCatalogPort, applicationRepository);
        validationOrder.verify(organizationRepository).findById(UUID.fromString(ORGANIZATION_ID));
        validationOrder.verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID);
        validationOrder.verify(recordExistsCatalogPort).exists(ReferenceCatalog.APPLICATION_STATE, STATE_ID);
        validationOrder.verify(applicationRepository).existsByNameAndOrganization("Message App", ORGANIZATION_ID);
    }

    @Test
    void validate_shortCircuitsSubsequentChecks_whenOrganizationDoesNotExist() {
        when(organizationRepository.findById(UUID.fromString(ORGANIZATION_ID)))
                .thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_151.getCode()))
                .thenReturn("La organización no existe.");
        CreateApplicationDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("La organización no existe."));

        verify(organizationRepository).findById(UUID.fromString(ORGANIZATION_ID));
        verifyNoInteractions(recordExistsCatalogPort, applicationRepository);
    }

    @Test
    void validate_shortCircuitsSubsequentChecks_whenOrganizationIdIsMissing() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_150.getCode()))
                .thenReturn("La organización es requerida.");
        CreateApplicationDTO dto = validDto();
        dto.setOrganizationId("");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("La organización es requerida."));

        verifyNoInteractions(organizationRepository, recordExistsCatalogPort, applicationRepository);
    }

    @Test
    void validate_shortCircuitsSubsequentChecks_whenOrganizationIdIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn("El identificador de organización no es un UUID válido.");
        CreateApplicationDTO dto = validDto();
        dto.setOrganizationId(INVALID_UUID);

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El identificador de organización no es un UUID válido."));

        verifyNoInteractions(organizationRepository, recordExistsCatalogPort, applicationRepository);
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
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_161.getCode()))
                .thenReturn("El nombre de la aplicación es requerido.");

        assertThatThrownBy(() -> validator.validate(new CreateApplicationDTO()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El nombre de la aplicación es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenNameExceedsMaxLength() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_162.getCode()))
                .thenReturn("El nombre de la aplicación no puede superar los 50 caracteres.");
        CreateApplicationDTO dto = validDto();
        dto.setName("a".repeat(51));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El nombre de la aplicación no puede superar los 50 caracteres."));
    }

    @Test
    void validate_throwsBusinessRule_whenLanguageIdIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_163.getCode()))
                .thenReturn("El idioma de la aplicación es requerido.");
        CreateApplicationDTO dto = validDto();
        dto.setLanguageId("");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El idioma de la aplicación es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenLanguageDoesNotExist() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_164.getCode()))
                .thenReturn("El idioma de la aplicación no existe.");
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID)).thenReturn(false);
        CreateApplicationDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El idioma de la aplicación no existe."));
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID);
        verify(recordExistsCatalogPort, never()).exists(eq(ReferenceCatalog.APPLICATION_STATE), anyString());
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenLanguageIdIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_164.getCode()))
                .thenReturn("El idioma de la aplicación no existe.");
        CreateApplicationDTO dto = validDto();
        dto.setLanguageId(INVALID_UUID);

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El idioma de la aplicación no existe."));
        verifyNoInteractions(recordExistsCatalogPort, applicationRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenStateIdIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_167.getCode()))
                .thenReturn("El estado de la aplicación es requerido.");
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        CreateApplicationDTO dto = validDto();
        dto.setStateId("");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la aplicación es requerido."));
    }

    @Test
    void validate_throwsBusinessRule_whenStateDoesNotExist() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_168.getCode()))
                .thenReturn("El estado de la aplicación no existe.");
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID)).thenReturn(true);
        when(recordExistsCatalogPort.exists(ReferenceCatalog.APPLICATION_STATE, STATE_ID)).thenReturn(false);
        CreateApplicationDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la aplicación no existe."));
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.APPLICATION_STATE, STATE_ID);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenStateIdIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_168.getCode()))
                .thenReturn("El estado de la aplicación no existe.");
        when(recordExistsCatalogPort.exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID)).thenReturn(true);
        CreateApplicationDTO dto = validDto();
        dto.setStateId(INVALID_UUID);

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El estado de la aplicación no existe."));
        verify(recordExistsCatalogPort).exists(ReferenceCatalog.LANGUAGE_BASE, LANGUAGE_ID);
        verify(recordExistsCatalogPort, never()).exists(eq(ReferenceCatalog.APPLICATION_STATE), anyString());
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenNameAlreadyExists() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_169.getCode()))
                .thenReturn("Ya existe una aplicación con el nombre proporcionado.");
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(applicationRepository.existsByNameAndOrganization("Message App", ORGANIZATION_ID)).thenReturn(true);
        CreateApplicationDTO dto = validDto();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Ya existe una aplicación con el nombre proporcionado."));
    }
}
