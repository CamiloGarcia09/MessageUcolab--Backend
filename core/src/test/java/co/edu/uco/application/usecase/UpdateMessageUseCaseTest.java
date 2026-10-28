package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.UpdateMessageCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageUseCaseTest {

    private static final String APP_UUID = "123e4567-e89b-12d3-a456-426614175020";
    private static final String DEVELOP_UUID = "123e4567-e89b-12d3-a456-426614175000";
    private static final String PRODUCTION_UUID = "123e4567-e89b-12d3-a456-426614175002";
    private static final String FUNC_UUID = "123e4567-e89b-12d3-a456-426614175001";
    private static final String TYPE_UUID = "123e4567-e89b-12d3-a456-426614175010";
    private static final String CATEGORY_UUID = "123e4567-e89b-12d3-a456-426614175011";
    private static final String STATUS_UUID = "123e4567-e89b-12d3-a456-426614175012";
    private static final String ACTIVE_STATE_UUID = "123e4567-e89b-12d3-a456-426614175013";
    private static final String MESSAGE_UUID = "123e4567-e89b-12d3-a456-426614175030";
    private static final String MESSAGE_CODE = "MSG_WELCOME";

    @Mock
    private UpdateMessageRepository updateMessageRepository;
    @Mock
    private DataBaseMessageRepository messageRepository;
    @Mock
    private UpdateMessageCompositeValidator validator;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository;
    @Mock
    private MessageCatalogStrategy messageCatalogStrategy;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private UpdateMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(UpdateMessageUseCase.class)).thenReturn(log);
        lenient().when(catalogPort.getMessage("TCH_095"))
                .thenReturn("Mensaje actualizado correctamente con el identificador: {}");
        lenient().when(catalogPort.getMessage("TCH_072")).thenReturn("Error al actualizar el mensaje.");
        useCase = new UpdateMessageUseCase(
                updateMessageRepository,
                messageRepository,
                validator,
                environmentResolver,
                messageEnvironmentStateRepository,
                messageCatalogStrategy,
                catalogPort,
                loggerFactory);
    }

    private UpdateMessageDTO validDto() {
        return UpdateMessageDTO.builder()
                .title("Updated title")
                .content("Updated content for the message")
                .typeId(TYPE_UUID)
                .categoryId(CATEGORY_UUID)
                .statusId(STATUS_UUID)
                .functionalityId(FUNC_UUID)
                .build();
    }

    private MessageAccessContext sessionContext() {
        return new MessageAccessContext(APP_UUID);
    }

    private MessageAccessContext authenticatedContext() {
        return new MessageAccessContext(APP_UUID);
    }

    private MessageData existingMessage() {
        return new MessageData(
                UUID.fromString(MESSAGE_UUID),
                MESSAGE_CODE,
                "Original title",
                "Original content",
                null,
                null,
                "",
                null
        );
    }

    private void stubApplicationResolvingTo(String applicationId) {
        when(environmentResolver.resolveApplication(any()))
                .thenReturn(applicationId);
    }

    private void stubExistingMessage() {
        when(messageRepository.findByCodeAndApplication(MESSAGE_CODE, APP_UUID))
                .thenReturn(Optional.of(existingMessage()));
    }

    private void stubActiveState() {
        when(messageEnvironmentStateRepository.findAll()).thenReturn(List.of(
                new MessageEnvironmentStateData(UUID.fromString(ACTIVE_STATE_UUID), STATE_ACTIVE)));
    }

    @Test
    void updateMessage_executesFullFlowInOrder() {
        UpdateMessageDTO dto = validDto();
        MessageAccessContext context = sessionContext();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();

        useCase.updateMessage(MESSAGE_CODE, dto, context);

        InOrder order = inOrder(environmentResolver, messageRepository, validator, updateMessageRepository);
        order.verify(environmentResolver).resolveApplication(context);
        order.verify(messageRepository).findByCodeAndApplication(MESSAGE_CODE, APP_UUID);
        order.verify(validator).validate(dto, APP_UUID);
        order.verify(updateMessageRepository).updateMessage(any(MessageData.class), isNull(), isNull());
        verify(log).info("Mensaje actualizado correctamente con el identificador: {}", MESSAGE_CODE);
    }

    @Test
    void updateMessage_keepsEnvironmentsWithoutQueryingStates_whenDtoHasNoEnvironments() {
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();

        useCase.updateMessage(MESSAGE_CODE, validDto(), sessionContext());

        verify(updateMessageRepository).updateMessage(any(MessageData.class), isNull(), isNull());
        verifyNoInteractions(messageEnvironmentStateRepository);
    }

    @Test
    void updateMessage_replacesEnvironmentsWithActiveState_whenDtoHasEnvironments() {
        UpdateMessageDTO dto = validDto();
        dto.setEnvironmentIds(List.of(DEVELOP_UUID.toUpperCase(), PRODUCTION_UUID));
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        stubActiveState();

        useCase.updateMessage(MESSAGE_CODE, dto, sessionContext());

        verify(updateMessageRepository).updateMessage(any(MessageData.class),
                eq(List.of(DEVELOP_UUID, PRODUCTION_UUID)), eq(ACTIVE_STATE_UUID));
    }

    @Test
    void updateMessage_throwsBusinessRuleUsingFun197_whenNoActiveStateToLinkEnvironments() {
        UpdateMessageDTO dto = validDto();
        dto.setEnvironmentIds(List.of(DEVELOP_UUID));
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        when(messageEnvironmentStateRepository.findAll()).thenReturn(List.of());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_197.getCode())).thenReturn("Sin estado activo");

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, sessionContext()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Sin estado activo"));
        verifyNoInteractions(updateMessageRepository);
    }

    @Test
    void updateMessage_usesExistingIdAndCodeInBuiltData() {
        UpdateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        ArgumentCaptor<MessageData> captor = ArgumentCaptor.forClass(MessageData.class);

        useCase.updateMessage(MESSAGE_CODE, dto, sessionContext());

        verify(updateMessageRepository).updateMessage(captor.capture(), isNull(), isNull());
        MessageData saved = captor.getValue();
        assertSoftly(softly -> {
            softly.assertThat(saved.getId()).isEqualTo(UUID.fromString(MESSAGE_UUID));
            softly.assertThat(saved.getCode()).isEqualTo(MESSAGE_CODE);
            softly.assertThat(saved.getTitle()).isEqualTo("Updated title");
            softly.assertThat(saved.getContent()).isEqualTo("Updated content for the message");
            softly.assertThat(saved.getType().getId()).isEqualTo(UUID.fromString(TYPE_UUID));
            softly.assertThat(saved.getCategory().getId()).isEqualTo(UUID.fromString(CATEGORY_UUID));
            softly.assertThat(saved.getStatus().getId()).isEqualTo(UUID.fromString(STATUS_UUID));
            softly.assertThat(saved.getFunctionality().getId()).isEqualTo(UUID.fromString(FUNC_UUID));
            softly.assertThat(saved.getFunctionality().getApplication().getId())
                    .isEqualTo(UUID.fromString(APP_UUID));
        });
    }

    @Test
    void updateMessage_worksWithAuthenticatedContext() {
        UpdateMessageDTO dto = validDto();
        MessageAccessContext context = authenticatedContext();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();

        useCase.updateMessage(MESSAGE_CODE, dto, context);

        verify(environmentResolver).resolveApplication(context);
        verify(validator).validate(dto, APP_UUID);
        verify(updateMessageRepository).updateMessage(any(MessageData.class), isNull(), isNull());
    }

    @Test
    void updateMessage_trimsMessageIdBeforeProcessing() {
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();

        useCase.updateMessage("  " + MESSAGE_CODE + "  ", validDto(), sessionContext());

        verify(messageRepository).findByCodeAndApplication(MESSAGE_CODE, APP_UUID);
        verify(log).info("Mensaje actualizado correctamente con el identificador: {}", MESSAGE_CODE);
    }

    @ParameterizedTest(name = "messageId blank: \"{0}\"")
    @ValueSource(strings = {"", " ", "  "})
    void updateMessage_throwsBusinessRule_whenMessageIdIsBlank(String blankId) {
        UpdateMessageDTO dto = validDto();
        MessageAccessContext context = sessionContext();
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_206.getCode()))
                .thenReturn("Id de mensaje requerido");

        assertThatThrownBy(() -> useCase.updateMessage(blankId, dto, context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Id de mensaje requerido"));

        verifyNoInteractions(environmentResolver, messageRepository, validator, updateMessageRepository);
    }

    @Test
    void updateMessage_throwsBusinessRule_whenMessageDoesNotExistInApplication() {
        UpdateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        when(messageRepository.findByCodeAndApplication(MESSAGE_CODE, APP_UUID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_208.getCode()))
                .thenReturn("Mensaje no encontrado");

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, sessionContext()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Mensaje no encontrado"));

        verifyNoInteractions(validator, updateMessageRepository);
    }

    @Test
    void updateMessage_propagatesValidationException() {
        UpdateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        doThrow(BusinessRuleException.buildUserException("Validación fallida"))
                .when(validator).validate(dto, APP_UUID);

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, sessionContext()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Validación fallida"));

        verifyNoInteractions(updateMessageRepository);
    }

    @Test
    void updateMessage_propagatesForbiddenFromApplicationResolver() {
        UpdateMessageDTO dto = validDto();
        MessageAccessContext context = authenticatedContext();
        ForbiddenException failure = ForbiddenException.buildUserException("Permission denied");
        doThrow(failure).when(environmentResolver).resolveApplication(context);

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, context)).isSameAs(failure);

        verifyNoInteractions(messageRepository, validator, updateMessageRepository);
    }

    @Test
    void updateMessage_throwsCrossWordsExceptionWithFun207_whenRepositoryFails() {
        UpdateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_207.getCode()))
                .thenReturn("No fue posible actualizar el mensaje.");
        doThrow(new RuntimeException("db error")).when(updateMessageRepository)
                .updateMessage(any(MessageData.class), isNull(), isNull());

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, sessionContext()))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("No fue posible actualizar el mensaje."));

        assertAll(
                () -> verify(log).error(eq("Error al actualizar el mensaje."),
                        any(RuntimeException.class)),
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_207.getCode())
        );
    }

    @Test
    void updateMessage_rethrowsDomainExceptionFromRepository() {
        UpdateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubExistingMessage();
        BusinessRuleException domain = BusinessRuleException.buildUserException("conflict");
        doThrow(domain).when(updateMessageRepository).updateMessage(any(MessageData.class), isNull(), isNull());

        assertThatThrownBy(() -> useCase.updateMessage(MESSAGE_CODE, dto, sessionContext())).isSameAs(domain);
    }
}
