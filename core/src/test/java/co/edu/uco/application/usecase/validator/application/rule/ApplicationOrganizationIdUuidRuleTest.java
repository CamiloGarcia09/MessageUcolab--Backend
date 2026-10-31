package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationOrganizationIdUuidRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String MALFORMED_UUID = "not-a-uuid";
    private static final String UUID_MESSAGE = "El identificador de organización no es un UUID válido.";

    @Mock
    private CatalogPort catalogPort;

    private ApplicationOrganizationIdUuidRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new ApplicationOrganizationIdUuidRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenOrganizationIdIsValidUuid() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(VALID_UUID).build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleExceptionUsingCatalogCode_whenOrganizationIdIsNotAUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn(UUID_MESSAGE);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(MALFORMED_UUID).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getUserMessage())
                        .isEqualTo(UUID_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_UUID, "", "   "})
    void validate_throwsBusinessRuleException_whenOrganizationIdIsDefaultOrBlank(String organizationId) {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(organizationId).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}
