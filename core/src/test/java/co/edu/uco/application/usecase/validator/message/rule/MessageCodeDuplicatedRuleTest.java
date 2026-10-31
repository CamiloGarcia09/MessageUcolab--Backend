package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageCodeDuplicatedRuleTest {

    private static final String APPLICATION_ID = "123e4567-e89b-12d3-a456-426614175106";
    private static final String FUN_200_USER_MESSAGE = "Ya existe un mensaje con ese código en la aplicación.";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private MessageCodeQueryPort messageCodeQueryPort;

    private MessageCodeDuplicatedRule rule;

    @BeforeEach
    void setUp() {
        rule = new MessageCodeDuplicatedRule(catalogPort, messageCodeQueryPort);
    }

    private static CreateMessageValidationContext contextWithCode(String code) {
        return new CreateMessageValidationContext(
                CreateMessageDTO.builder().code(code).build(), APPLICATION_ID);
    }

    @Test
    void validate_doesNotThrow_whenNormalizedCodeIsNotRegisteredForTheApplication() {
        when(messageCodeQueryPort.existsByCodeAndApplicationId("MSG_WELCOME", APPLICATION_ID))
                .thenReturn(false);

        assertThatCode(() -> rule.validate(contextWithCode("WELCOME")))
                .doesNotThrowAnyException();

        assertAll(
                () -> verify(messageCodeQueryPort)
                        .existsByCodeAndApplicationId("MSG_WELCOME", APPLICATION_ID),
                () -> verifyNoInteractions(catalogPort));
    }

    @Test
    void validate_queriesNormalizedCode_whenRawSuffixUsesLowerCaseAndConsecutiveSpaces() {
        when(messageCodeQueryPort.existsByCodeAndApplicationId(
                "MSG_WEL_COME", APPLICATION_ID)).thenReturn(false);

        assertThatCode(() -> rule.validate(contextWithCode("wel  come")))
                .doesNotThrowAnyException();

        verify(messageCodeQueryPort)
                .existsByCodeAndApplicationId("MSG_WEL_COME", APPLICATION_ID);
    }

    @Test
    void validate_queriesPrefixedCode_whenContextCodeIsNull() {
        when(messageCodeQueryPort.existsByCodeAndApplicationId("MSG_", APPLICATION_ID))
                .thenReturn(false);

        assertThatCode(() -> rule.validate(contextWithCode(null)))
                .doesNotThrowAnyException();

        verify(messageCodeQueryPort).existsByCodeAndApplicationId("MSG_", APPLICATION_ID);
    }

    @Test
    void validate_throwsConflictUsingFun200_whenCodeIsAlreadyRegisteredForTheApplication() {
        when(messageCodeQueryPort.existsByCodeAndApplicationId("MSG_WELCOME", APPLICATION_ID))
                .thenReturn(true);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_200.getCode()))
                .thenReturn(FUN_200_USER_MESSAGE);

        assertThatThrownBy(() -> rule.validate(contextWithCode("WELCOME")))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat((ConflictException) exception)
                        .extracting(ConflictException::getHttpStatus, ConflictException::getUserMessage)
                        .containsExactly(409, FUN_200_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_200.getCode());
    }
}
