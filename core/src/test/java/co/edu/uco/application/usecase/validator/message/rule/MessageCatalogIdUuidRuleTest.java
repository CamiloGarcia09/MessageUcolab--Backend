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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageCatalogIdUuidRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175101";
    private static final String UPPERCASE_UUID = "123E4567-E89B-12D3-A456-426614175101";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String MALFORMED_UUID = "not-a-uuid";
    private static final String FUN_038_USER_MESSAGE = "El id del catálogo debe ser un UUID válido y distinto del UUID por defecto.";

    @Mock
    private CatalogPort catalogPort;

    private static Stream<Arguments> catalogIdFields() {
        return Stream.of(
                Arguments.of("El tipo de mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getTypeId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setTypeId),
                Arguments.of("La categoría del mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getCategoryId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setCategoryId),
                Arguments.of("El estado del mensaje",
                        (Function<CreateMessageDTO, String>) CreateMessageDTO::getStatusId,
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setStatusId));
    }

    private static Stream<Arguments> catalogIdExtractors() {
        return catalogIdFields().map(arguments -> Arguments.of(arguments.get()[0], arguments.get()[1]));
    }

    private MessageCatalogIdUuidRule rule(Function<CreateMessageDTO, String> extractor) {
        return new MessageCatalogIdUuidRule(catalogPort, extractor);
    }

    private void stubFun038Message() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn(FUN_038_USER_MESSAGE);
    }

    private static void assertFun038Failure(Throwable thrown) {
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenCatalogIdIsWellFormedNonDefaultUuid(String messagePrefix,
                                                                        Function<CreateMessageDTO, String> extractor,
                                                                        BiConsumer<CreateMessageDTO, String> setter) {
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatCode(() -> rule(extractor).validate(dto))
                .as("Debe aceptarse %s con UUID válido y distinto del UUID por defecto", messagePrefix)
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenCatalogIdIsUppercaseUuid(String messagePrefix,
                                                             Function<CreateMessageDTO, String> extractor,
                                                             BiConsumer<CreateMessageDTO, String> setter) {
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, UPPERCASE_UUID);

        assertThatCode(() -> rule(extractor).validate(dto))
                .as("Debe aceptarse %s con UUID en mayúsculas", messagePrefix)
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsMalformedUuid(String messagePrefix,
                                                                             Function<CreateMessageDTO, String> extractor,
                                                                             BiConsumer<CreateMessageDTO, String> setter) {
        stubFun038Message();
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, MALFORMED_UUID);

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .as("Debe rechazarse %s con texto que no es un UUID", messagePrefix)
                .satisfies(MessageCatalogIdUuidRuleTest::assertFun038Failure);

        verify(catalogPort).getMessage("FUN_038");
        verifyNoMoreInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsDefaultUuid(String messagePrefix,
                                                                           Function<CreateMessageDTO, String> extractor,
                                                                           BiConsumer<CreateMessageDTO, String> setter) {
        stubFun038Message();
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, DEFAULT_UUID);

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .as("Debe rechazarse %s con el UUID por defecto", messagePrefix)
                .satisfies(MessageCatalogIdUuidRuleTest::assertFun038Failure);

        verify(catalogPort).getMessage("FUN_038");
        verifyNoMoreInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsBlank(String messagePrefix,
                                                                     Function<CreateMessageDTO, String> extractor,
                                                                     BiConsumer<CreateMessageDTO, String> setter) {
        stubFun038Message();
        CreateMessageDTO dto = new CreateMessageDTO();
        setter.accept(dto, "");

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .as("Debe rechazarse %s en blanco", messagePrefix)
                .satisfies(MessageCatalogIdUuidRuleTest::assertFun038Failure);

        verify(catalogPort).getMessage("FUN_038");
        verifyNoMoreInteractions(catalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdExtractors")
    void validate_throwsBusinessRuleWithFun038_whenCatalogIdIsNull(String messagePrefix,
                                                                    Function<CreateMessageDTO, String> extractor) {
        stubFun038Message();
        CreateMessageDTO dto = CreateMessageDTO.builder().build();
        assertThat(extractor.apply(dto))
                .as("Precondición: %s debe estar ausente en el DTO", messagePrefix)
                .isNull();

        assertThatThrownBy(() -> rule(extractor).validate(dto))
                .as("Debe rechazarse %s ausente", messagePrefix)
                .satisfies(MessageCatalogIdUuidRuleTest::assertFun038Failure);

        verify(catalogPort).getMessage("FUN_038");
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleWithFun038_whenDtoIsNull() {
        stubFun038Message();

        assertThatThrownBy(() -> rule(CreateMessageDTO::getTypeId).validate(null))
                .as("Debe rechazarse la ausencia total del DTO")
                .satisfies(MessageCatalogIdUuidRuleTest::assertFun038Failure);

        verify(catalogPort).getMessage("FUN_038");
        verifyNoMoreInteractions(catalogPort);
    }
}
