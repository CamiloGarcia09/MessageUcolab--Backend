package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageCatalogIdRequiredRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175101";

    @Mock
    private CatalogPort catalogPort;

    private static Stream<Arguments> catalogIdFields() {
        return Stream.of(
                Arguments.of("El tipo de mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getTypeId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setTypeId,
                        MessageCatalogCodeEnum.FUN_190),
                Arguments.of("La categoría del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getCategoryId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setCategoryId,
                        MessageCatalogCodeEnum.FUN_192),
                Arguments.of("El estado del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getStatusId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setStatusId,
                        MessageCatalogCodeEnum.FUN_194));
    }

    private UpdateMessageCatalogIdRequiredRule rule(Function<UpdateMessageDTO, String> extractor,
                                                    MessageCatalogCodeEnum code) {
        return new UpdateMessageCatalogIdRequiredRule(catalogPort, extractor, code);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenCatalogIdIsPresent(String messagePrefix,
                                                      Function<UpdateMessageDTO, String> extractor,
                                                      BiConsumer<UpdateMessageDTO, String> setter,
                                                      MessageCatalogCodeEnum code) {
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatCode(() -> rule(extractor, code).validate(dto))
                .as("Debe aceptarse %s con id de catálogo válido", messagePrefix)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithExactCode_whenCatalogIdIsEmpty(String messagePrefix,
                                                                       Function<UpdateMessageDTO, String> extractor,
                                                                       BiConsumer<UpdateMessageDTO, String> setter,
                                                                       MessageCatalogCodeEnum code) {
        when(catalogPort.getMessage(code.getCode())).thenReturn(messagePrefix + " es requerido.");
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, "");

        UpdateMessageCatalogIdRequiredRule ruleUnderTest = rule(extractor, code);

        assertThatThrownBy(() -> ruleUnderTest.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(messagePrefix + " es requerido."));
        verify(catalogPort).getMessage(code.getCode());
    }

    @Test
    void validate_throwsBusinessRuleWithExactCode_whenCatalogIdIsAbsent() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_190.getCode()))
                .thenReturn("El tipo de mensaje es requerido.");
        UpdateMessageDTO dto = new UpdateMessageDTO();

        UpdateMessageCatalogIdRequiredRule ruleUnderTest = rule(UpdateMessageDTO::getTypeId,
                MessageCatalogCodeEnum.FUN_190);

        assertThatThrownBy(() -> ruleUnderTest.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El tipo de mensaje es requerido."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_190.getCode());
    }

    @Test
    void validate_throwsBusinessRuleWithExactCode_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_190.getCode()))
                .thenReturn("El tipo de mensaje es requerido.");

        UpdateMessageCatalogIdRequiredRule ruleUnderTest = rule(UpdateMessageDTO::getTypeId,
                MessageCatalogCodeEnum.FUN_190);

        assertThatThrownBy(() -> ruleUnderTest.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El tipo de mensaje es requerido."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_190.getCode());
    }
}
