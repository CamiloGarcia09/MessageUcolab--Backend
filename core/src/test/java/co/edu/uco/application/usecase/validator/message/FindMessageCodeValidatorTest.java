package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMessageCodeValidatorTest {

    @Mock
    private CatalogPort catalogPort;

    @InjectMocks
    private FindMessageCodeValidator validator;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validate_throwsBusinessRuleException_forBlankCode(String code) {
        when(catalogPort.getMessage("FUN_040")).thenReturn("Message code cannot be empty");

        assertThatThrownBy(() -> validator.validate(code))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Message code cannot be empty"));
    }

    @Test
    void validate_doesNotThrow_forNonBlankCode() {
        assertThatCode(() -> validator.validate("MSG_001")).doesNotThrowAnyException();
    }
}