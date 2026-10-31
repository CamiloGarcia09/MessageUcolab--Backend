package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetLanguageFormatRuleTest {

    private static final String INVALID_FORMAT_MESSAGE = "Target language has invalid format";
    private static final String FIFTY_CHARS =
            "abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy";
    private static final String FIFTY_ONE_CHARS = FIFTY_CHARS + "z";

    @Mock
    private CatalogPort catalogPort;

    private TargetLanguageFormatRule rule;

    @BeforeEach
    void setUp() {
        rule = new TargetLanguageFormatRule(catalogPort);
    }

    @ParameterizedTest(name = "language=[{0}]")
    @ValueSource(strings = {"en", "es", "English", "es-LA", "zh-Hans", "pt_BR", "en_US",
            "  en  ", " es-LA ", FIFTY_CHARS})
    void validate_doesNotThrow_whenLanguageMatchesTheExpectedFormat(String language) {
        assertThatCode(() -> rule.validate(language)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "language=[{0}]")
    @ValueSource(strings = {"1abc", "en 2", "_", "-", "a", "es-419", "@@", FIFTY_ONE_CHARS})
    void validate_throwsBusinessRuleUsingFun045_whenLanguageDoesNotMatchTheFormat(String language) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_045.getCode()))
                .thenReturn(INVALID_FORMAT_MESSAGE);

        assertThatThrownBy(() -> rule.validate(language))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, INVALID_FORMAT_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_045.getCode());
    }

    @ParameterizedTest(name = "language=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void validate_throwsBusinessRuleUsingFun045_whenLanguageIsBlank(String language) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_045.getCode()))
                .thenReturn(INVALID_FORMAT_MESSAGE);

        assertThatThrownBy(() -> rule.validate(language))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage)
                        .isEqualTo(INVALID_FORMAT_MESSAGE));
    }
}
