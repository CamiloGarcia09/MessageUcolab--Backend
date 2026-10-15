package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
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
class UpdateMessageCatalogIdExistsRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175101";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String MALFORMED_UUID = "not-a-uuid";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;

    private static Stream<Arguments> catalogIdFields() {
        return Stream.of(
                Arguments.of("El tipo de mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getTypeId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setTypeId,
                        ReferenceCatalog.MESSAGE_TYPE, MessageCatalogCodeEnum.FUN_191),
                Arguments.of("La categoría del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getCategoryId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setCategoryId,
                        ReferenceCatalog.MESSAGE_CATEGORY, MessageCatalogCodeEnum.FUN_193),
                Arguments.of("El estado del mensaje",
                        (Function<UpdateMessageDTO, String>) UpdateMessageDTO::getStatusId,
                        (BiConsumer<UpdateMessageDTO, String>) UpdateMessageDTO::setStatusId,
                        ReferenceCatalog.MESSAGE_STATE, MessageCatalogCodeEnum.FUN_195));
    }

    private UpdateMessageCatalogIdExistsRule rule(Function<UpdateMessageDTO, String> extractor,
                                                   ReferenceCatalog referenceCatalog,
                                                   MessageCatalogCodeEnum code) {
        return new UpdateMessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort, extractor,
                referenceCatalog, code);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_doesNotThrow_whenReferenceExistsForExactCatalogAndUuid(String messagePrefix,
                                                                          Function<UpdateMessageDTO, String> extractor,
                                                                          BiConsumer<UpdateMessageDTO, String> setter,
                                                                          ReferenceCatalog referenceCatalog,
                                                                          MessageCatalogCodeEnum code) {
        when(recordExistsCatalogPort.exists(referenceCatalog, VALID_UUID)).thenReturn(true);
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatCode(() -> rule(extractor, referenceCatalog, code).validate(dto))
                .doesNotThrowAnyException();

        verify(recordExistsCatalogPort).exists(referenceCatalog, VALID_UUID);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithExactCode_whenReferenceDoesNotExist(String messagePrefix,
                                                                             Function<UpdateMessageDTO, String> extractor,
                                                                             BiConsumer<UpdateMessageDTO, String> setter,
                                                                             ReferenceCatalog referenceCatalog,
                                                                             MessageCatalogCodeEnum code) {
        when(recordExistsCatalogPort.exists(referenceCatalog, VALID_UUID)).thenReturn(false);
        when(catalogPort.getMessage(code.getCode())).thenReturn(messagePrefix + " no existe.");
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, VALID_UUID);

        assertThatThrownBy(() -> rule(extractor, referenceCatalog, code).validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(messagePrefix + " no existe."));

        verify(catalogPort).getMessage(code.getCode());
        verify(recordExistsCatalogPort).exists(referenceCatalog, VALID_UUID);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithExactCode_withoutRepositoryLookup_whenReferenceIsMalformedUuid(
            String messagePrefix,
            Function<UpdateMessageDTO, String> extractor,
            BiConsumer<UpdateMessageDTO, String> setter,
            ReferenceCatalog referenceCatalog,
            MessageCatalogCodeEnum code) {
        when(catalogPort.getMessage(code.getCode())).thenReturn(messagePrefix + " no existe.");
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, MALFORMED_UUID);

        assertThatThrownBy(() -> rule(extractor, referenceCatalog, code).validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(messagePrefix + " no existe."));

        verify(catalogPort).getMessage(code.getCode());
        verifyNoInteractions(recordExistsCatalogPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogIdFields")
    void validate_throwsBusinessRuleWithExactCode_withoutRepositoryLookup_whenReferenceIsDefaultUuid(
            String messagePrefix,
            Function<UpdateMessageDTO, String> extractor,
            BiConsumer<UpdateMessageDTO, String> setter,
            ReferenceCatalog referenceCatalog,
            MessageCatalogCodeEnum code) {
        when(catalogPort.getMessage(code.getCode())).thenReturn(messagePrefix + " no existe.");
        UpdateMessageDTO dto = new UpdateMessageDTO();
        setter.accept(dto, DEFAULT_UUID);

        assertThatThrownBy(() -> rule(extractor, referenceCatalog, code).validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(messagePrefix + " no existe."));

        verify(catalogPort).getMessage(code.getCode());
        verifyNoInteractions(recordExistsCatalogPort);
    }

    @Test
    void validate_throwsBusinessRuleWithExactCode_withoutRepositoryLookup_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_191.getCode()))
                .thenReturn("El tipo de mensaje no existe.");

        assertThatThrownBy(() -> rule(UpdateMessageDTO::getTypeId,
                ReferenceCatalog.MESSAGE_TYPE, MessageCatalogCodeEnum.FUN_191).validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("El tipo de mensaje no existe."));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_191.getCode());
        verifyNoInteractions(recordExistsCatalogPort);
    }
}
