package co.edu.uco.application.usecase.validator.message.rule;

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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerLanguageCodeKnownRuleTest {

    private static final String FUN_045_USER_MESSAGE = "El idioma destino no es válido.";

    @Mock
    private CatalogPort catalogPort;

    private ConsumerLanguageCodeKnownRule rule;

    @BeforeEach
    void setUp() {
        rule = new ConsumerLanguageCodeKnownRule(catalogPort, MessageCatalogCodeEnum.FUN_045);
    }

    @ParameterizedTest(name = "acepta la etiqueta \"{0}\"")
    @ValueSource(strings = {"es", "en", "EN", "pt-br", "pt_BR", "zh-hans", "zh-hans-cn", "es-419", "fra", " fr "})
    void validate_doesNotThrow_whenTagIsARealLanguage(String languageTag) {
        assertThatCode(() -> rule.validate(languageTag))
                .as("Debe aceptarse la etiqueta \"%s\"", languageTag)
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "rechaza la etiqueta \"{0}\"")
    @ValueSource(strings = {"fr-aa1", "xx", "zz-zz", "english", "es-", "es-xx", "es-latn-latn", "es-419-us-extra", "1234"})
    @NullAndEmptySource
    void validate_throwsBusinessRuleUsingFun045_whenTagIsNotARealLanguage(String languageTag) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_045.getCode())).thenReturn(FUN_045_USER_MESSAGE);

        assertThatThrownBy(() -> rule.validate(languageTag))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_045_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_045.getCode());
    }

    @Test
    void validate_rejectsAnInventedTagThatWouldTriggerANewModelCall() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_045.getCode())).thenReturn(FUN_045_USER_MESSAGE);

        assertThatThrownBy(() -> rule.validate("fr-aa1"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
