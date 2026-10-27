package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerLanguageCodeFormatRuleTest {
    @Mock private CatalogPort catalog;

    @ParameterizedTest
    @ValueSource(strings = {"en", "eng", "zh-Hans", "pt-BR", "es-419"})
    void validate_acceptsLanguageCodeSyntax(String code) {
        var rule = new ConsumerLanguageCodeFormatRule(catalog, MessageCatalogCodeEnum.FUN_045);

        assertThatCode(() -> rule.validate(code)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"English", "x", "en-123456789", "en--US", "en_US"})
    void validate_rejectsInvalidLanguageCodeSyntax(String code) {
        when(catalog.getMessage(MessageCatalogCodeEnum.FUN_045.getCode())).thenReturn("invalid language code");
        var rule = new ConsumerLanguageCodeFormatRule(catalog, MessageCatalogCodeEnum.FUN_045);

        assertThatThrownBy(() -> rule.validate(code)).isInstanceOf(BusinessRuleException.class);
    }
}
