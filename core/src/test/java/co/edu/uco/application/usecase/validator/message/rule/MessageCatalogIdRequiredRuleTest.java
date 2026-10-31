package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
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
class MessageCatalogIdRequiredRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175101";

    @Mock
    private CatalogPort catalogPort;

    private static Stream<Arguments> catalogIdFields() {
        return Stream.of(
                Arguments.of("El tipo de mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getTypeId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setTypeId,
                        MessageCatalogCodeEnum.FUN_190),
                Arguments.of("La categoría del mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getCategoryId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setCategoryId,
                        MessageCatalogCodeEnum.FUN_192),
                Arguments.of("El estado del mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getStatusId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setStatusId,
                        MessageCatalogCodeEnum.FUN_194));
    }

    private MessageCatalogIdRequiredRule rule(Function<CreateMessageDTO, String> extractor,
                                              MessageCatalogCodeEnum code) {
        return new MessageCatalogIdRequiredRule(catalogPort, extractor, code);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenCatalogIdIsPresent(String messagePrefix,
                                                      Function<CreateMessageDTO, String> extractor,
                                                      BiConsumer<CreateMessageDTO, String> setter,
                                                      MessageCatalogCodeEnum code) {
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatCode(() -> rule(extractor, code).validate(dto))
                .as("Debe aceptarse %s con id de catálogo válido", messagePrefix)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithExactCode_whenCatalogIdIsEmpty(String messagePrefix,
                                                                       Function<CreateMessageDTO, String> extractor,
                                                                       BiConsumer<CreateMessageDTO, String> setter,
                                                                       MessageCatalogCodeEnum code) {
        when(catalogPort.getMessage(code.getCode())).thenReturn(messagePrefix + " es requerido.");
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, "");

        MessageCatalogIdRequiredRule ruleUnderTest = rule(extractor, code);

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
        CreateMessageDTO dto = new CreateMessageDTO();

        MessageCatalogIdRequiredRule ruleUnderTest = rule(CreateMessageDTO::getTypeId,
                MessageCatalogCodeEnum.FUN_190);

        assertThatThrownBy(() -> ruleUnderTest.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El tipo de mensaje es requerido."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_190.getCode());
    }

    @Test
    void validate_throwsBusinessRuleWithExactCode_whenTypeIdIsBlank() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_190.getCode()))
                .thenReturn("El tipo de mensaje es requerido.");
        CreateMessageDTO dto = CreateMessageDTO.builder().typeId("   ").build();

        MessageCatalogIdRequiredRule ruleUnderTest = rule(CreateMessageDTO::getTypeId,
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

        MessageCatalogIdRequiredRule ruleUnderTest = rule(CreateMessageDTO::getTypeId,
                MessageCatalogCodeEnum.FUN_190);

        assertThatThrownBy(() -> ruleUnderTest.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El tipo de mensaje es requerido."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_190.getCode());
    }
}
