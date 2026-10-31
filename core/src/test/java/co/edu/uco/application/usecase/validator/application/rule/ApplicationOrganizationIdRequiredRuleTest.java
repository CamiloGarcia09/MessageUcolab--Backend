package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
class ApplicationOrganizationIdRequiredRuleTest {

    private static final String ORGANIZATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String REQUIRED_MESSAGE = "La organización es requerida.";

    @Mock
    private CatalogPort catalogPort;

    private ApplicationOrganizationIdRequiredRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new ApplicationOrganizationIdRequiredRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenOrganizationIdIsPresent() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(ORGANIZATION_ID).build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validate_throwsBusinessRuleExceptionUsingCatalogCode_whenOrganizationIdIsBlank(String organizationId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_150.getCode())).thenReturn(REQUIRED_MESSAGE);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(organizationId).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getUserMessage())
                        .isEqualTo(REQUIRED_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_150.getCode());
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}
