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
class MessageCodeMaxLengthRuleTest {

    private static final String FUN_198_USER_MESSAGE =
            "El sufijo del código del mensaje supera los 10 caracteres.";

    @Mock
    private CatalogPort catalogPort;

    private MessageCodeMaxLengthRule rule;

    @BeforeEach
    void setUp() {
        rule = new MessageCodeMaxLengthRule(catalogPort);
    }

    private void stubFun198Message() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_198.getCode()))
                .thenReturn(FUN_198_USER_MESSAGE);
    }

    private static void assertFun198Failure(Throwable thrown) {
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat((BusinessRuleException) ex)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FUN_198_USER_MESSAGE));
    }

    @ParameterizedTest(name = "sufijo \"{0}\" dentro del límite")
    @ValueSource(strings = {"W", "WELCOME", "ABCDEFGHIJ"})
    void validate_doesNotThrow_whenSuffixIsAtMostTenCharacters(String code) {
        CreateMessageDTO dto = CreateMessageDTO.builder().code(code).build();

        assertThatCode(() -> rule.validate(dto))
                .as("Debe aceptarse el sufijo \"%s\"", code)
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenOnlySurroundingWhitespaceExceedsTheLimit() {
        CreateMessageDTO dto = CreateMessageDTO.builder().code("  ABCDEFGHIJ  ").build();

        assertThatCode(() -> rule.validate(dto))
                .as("Los espacios envolventes no deben contar para la longitud máxima")
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenCodeIsEmpty() {
        CreateMessageDTO dto = CreateMessageDTO.builder().code("").build();

        assertThatCode(() -> rule.validate(dto))
                .as("La regla de longitud deja el vacío a la regla de obligatoriedad")
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun198_whenSuffixExceedsTenCharacters() {
        stubFun198Message();
        CreateMessageDTO dto = CreateMessageDTO.builder().code("ABCDEFGHIJK").build();

        assertThatThrownBy(() -> rule.validate(dto))
                .satisfies(MessageCodeMaxLengthRuleTest::assertFun198Failure);

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_198.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun198_whenDtoIsNull() {
        stubFun198Message();

        assertThatThrownBy(() -> rule.validate(null))
                .satisfies(MessageCodeMaxLengthRuleTest::assertFun198Failure);

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_198.getCode());
    }
}
