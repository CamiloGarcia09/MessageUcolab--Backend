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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetLanguageRequiredRuleTest {

    private static final String REQUIRED_MESSAGE = "Target language cannot be empty";

    @Mock
    private CatalogPort catalogPort;

    private TargetLanguageRequiredRule rule;

    @BeforeEach
    void setUp() {
        rule = new TargetLanguageRequiredRule(catalogPort);
    }

    @ParameterizedTest(name = "language=[{0}]")
    @ValueSource(strings = {"en", "es", "English", "es-LA"})
    void validate_doesNotThrow_whenLanguageIsPresent(String language) {
        assertThatCode(() -> rule.validate(language)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "language=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validate_throwsBusinessRuleUsingFun044_whenLanguageIsBlank(String language) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_044.getCode()))
                .thenReturn(REQUIRED_MESSAGE);

        assertThatThrownBy(() -> rule.validate(language))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, REQUIRED_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_044.getCode());
    }
}
