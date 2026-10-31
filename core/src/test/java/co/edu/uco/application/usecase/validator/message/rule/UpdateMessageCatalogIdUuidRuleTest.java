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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageCatalogIdUuidRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175101";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String MALFORMED_UUID = "not-a-uuid";
    private static final String FUN_038_USER_MESSAGE =
            "El id del catálogo debe ser un UUID válido y distinto del UUID por defecto.";

    @Mock
    private CatalogPort catalogPort;

    private static Stream<Arguments> catalogIdFields() {
        return Stream.of(
                Arguments.of("El tipo de mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getTypeId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setTypeId),
                Arguments.of("La categoría del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getCategoryId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setCategoryId),
                Arguments.of("El estado del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getStatusId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setStatusId));
    }

    private UpdateMessageCatalogIdUuidRule rule(Function<UpdateMessageDTO, String> extractor) {
        return new UpdateMessageCatalogIdUuidRule(catalogPort, extractor);
    }

    private void stubFun038Message() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn(FUN_038_USER_MESSAGE);
    }

    @ParameterizedTest(name = "{0} — valid UUID")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenCatalogIdIsWellFormedNonDefaultUuid(String messagePrefix,
                                                                        Function<UpdateMessageDTO, String> extractor,
                                                                        BiConsumer<UpdateMessageDTO, String> setter) {
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatCode(() -> rule(extractor).validate(dto))
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0} — malformed UUID")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsMalformedUuid(String messagePrefix,
                                                                             Function<UpdateMessageDTO, String> extractor,
                                                                             BiConsumer<UpdateMessageDTO, String> setter) {
        stubFun038Message();
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, MALFORMED_UUID);

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }

    @ParameterizedTest(name = "{0} — default UUID")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsDefaultUuid(String messagePrefix,
                                                                           Function<UpdateMessageDTO, String> extractor,
                                                                           BiConsumer<UpdateMessageDTO, String> setter) {
        stubFun038Message();
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, DEFAULT_UUID);

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }

    @Test
    void validate_throwsBusinessRuleWithFun038_whenDtoIsNull() {
        stubFun038Message();

        assertThatThrownBy(() -> rule(UpdateMessageDTO::getTypeId).validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }
}
