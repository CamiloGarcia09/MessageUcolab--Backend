package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageCodeFormatRuleTest {

    private static final String FUN_199_USER_MESSAGE =
            "El sufijo del código del mensaje solo admite letras, dígitos y espacios.";

    @Mock
    private CatalogPort catalogPort;

    private MessageCodeFormatRule rule;

    @BeforeEach
    void setUp() {
        rule = new MessageCodeFormatRule(catalogPort);
    }

    private void stubFun199Message() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_199.getCode()))
                .thenReturn(FUN_199_USER_MESSAGE);
    }

    private static void assertFun199Failure(Throwable thrown) {
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat((BusinessRuleException) ex)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FUN_199_USER_MESSAGE));
    }

    @ParameterizedTest(name = "sufijo \"{0}\" con formato admitido")
    @ValueSource(strings = {"WELCOME", "welcome 1", "abc123", "W1", "MENSAJE BIENVENIDO"})
    void validate_doesNotThrow_whenSuffixMatchesAllowedFormat(String code) {
        CreateMessageDTO dto = CreateMessageDTO.builder().code(code).build();

        assertThatCode(() -> rule.validate(dto))
                .as("Debe aceptarse el sufijo \"%s\"", code)
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "sufijo \"{0}\" con carácter no admitido")
    @ValueSource(strings = {"MSG-001", "HELLO_WORLD", "HOLA.MUNDO", "CODIGO#1"})
    void validate_throwsBusinessRuleUsingFun199_whenSuffixContainsDisallowedCharacter(String code) {
        stubFun199Message();
        CreateMessageDTO dto = CreateMessageDTO.builder().code(code).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .as("Debe rechazarse el sufijo \"%s\"", code)
                .satisfies(MessageCodeFormatRuleTest::assertFun199Failure);

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_199.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun199_whenCodeIsEmpty() {
        stubFun199Message();
        CreateMessageDTO dto = CreateMessageDTO.builder().code("").build();

        assertThatThrownBy(() -> rule.validate(dto))
                .satisfies(MessageCodeFormatRuleTest::assertFun199Failure);

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_199.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun199_whenDtoIsNull() {
        stubFun199Message();

        assertThatThrownBy(() -> rule.validate(null))
                .satisfies(MessageCodeFormatRuleTest::assertFun199Failure);

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_199.getCode());
    }
}
