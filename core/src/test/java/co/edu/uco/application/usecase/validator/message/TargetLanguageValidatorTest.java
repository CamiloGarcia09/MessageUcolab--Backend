package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetLanguageValidatorTest {

    @Mock
    private CatalogPort catalogPort;

    @InjectMocks
    private TargetLanguageValidator validator;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validate_throwsBusinessRuleException_forBlankLanguage(String language) {
        when(catalogPort.getMessage("FUN_044")).thenReturn("Target language cannot be empty");

        assertThatThrownBy(() -> validator.validate(language))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Target language cannot be empty"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1abc", "en 2", "_", "-", "a", "es-419", "@@"})
    void validate_throwsBusinessRuleException_forInvalidFormat(String language) {
        when(catalogPort.getMessage("FUN_045")).thenReturn("Target language has invalid format");

        assertThatThrownBy(() -> validator.validate(language))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Target language has invalid format"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "es", "English", "es-LA", "zh-Hans", "pt_BR", "en_US"})
    void validate_doesNotThrow_forValidLanguage(String language) {
        assertThatCode(() -> validator.validate(language)).doesNotThrowAnyException();
    }

    @Test
    void validate_trimsValueBeforeValidation() {
        assertThatCode(() -> validator.validate("  en  ")).doesNotThrowAnyException();
    }
}