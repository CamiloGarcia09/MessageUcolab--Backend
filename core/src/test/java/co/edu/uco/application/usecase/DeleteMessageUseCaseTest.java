package co.edu.uco.application.usecase;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteMessageUseCaseTest {

    private static final String MESSAGE_UUID = "123e4567-e89b-12d3-a456-426614175030";
    private static final String MESSAGE_CODE = "MSG_WELCOME";
    private static final ExternalIdentity IDENTITY = new ExternalIdentity(
            "issuer", "subject", "user@example.com", Instant.MAX);

    @Mock
    private DeleteMessageRepository deleteMessageRepository;
    @Mock
    private DataBaseMessageRepository messageRepository;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private DeleteMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(DeleteMessageUseCase.class)).thenReturn(log);
        useCase = new DeleteMessageUseCase(
                deleteMessageRepository,
                messageRepository,
                environmentResolver,
                catalogPort,
                loggerFactory);
    }

    private MessageAccessContext workspaceContext() {
        return new MessageAccessContext("env-1", null);
    }

    private MessageAccessContext authenticatedContext() {
        return new MessageAccessContext(null, IDENTITY);
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

    private void stubExistingMessage() {
        when(messageRepository.findByCodeAndApplication(eq(MESSAGE_CODE), any())).thenReturn(Optional.of(existingMessage()));
    }

    @Test
    void deleteMessage_executesFullFlowInOrder() {
        MessageAccessContext context = workspaceContext();
        stubExistingMessage();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");

        useCase.deleteMessage(MESSAGE_CODE, context);

        InOrder order = inOrder(environmentResolver, messageRepository, deleteMessageRepository);
        order.verify(environmentResolver).resolveApplication(context, PermissionCode.MESSAGE_DELETE);
        order.verify(messageRepository).findByCodeAndApplication(eq(MESSAGE_CODE), any());
        order.verify(deleteMessageRepository).deleteMessage(MESSAGE_UUID);
        verify(log).info("Message deleted successfully with id: {}", MESSAGE_CODE);
    }

    @Test
    void deleteMessage_deletesUsingExistingMessageUuid() {
        MessageAccessContext context = workspaceContext();
        stubExistingMessage();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");

        useCase.deleteMessage(MESSAGE_CODE, context);

        verify(deleteMessageRepository).deleteMessage(MESSAGE_UUID);
    }

    @Test
    void deleteMessage_trimsMessageIdBeforeProcessing() {
        MessageAccessContext context = workspaceContext();
        when(messageRepository.findByCodeAndApplication(eq(MESSAGE_CODE), any())).thenReturn(Optional.of(existingMessage()));
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");

        useCase.deleteMessage("  " + MESSAGE_CODE + "  ", context);

        verify(messageRepository).findByCodeAndApplication(eq(MESSAGE_CODE), any());
        verify(log).info("Message deleted successfully with id: {}", MESSAGE_CODE);
    }

    @Test
    void deleteMessage_worksWithAuthenticatedContext() {
        MessageAccessContext context = authenticatedContext();
        stubExistingMessage();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-auth");

        useCase.deleteMessage(MESSAGE_CODE, context);

        verify(environmentResolver).resolveApplication(context, PermissionCode.MESSAGE_DELETE);
        verify(deleteMessageRepository).deleteMessage(MESSAGE_UUID);
    }

    @ParameterizedTest(name = "messageId blank: \"{0}\"")
    @ValueSource(strings = {"", " ", "  "})
    void deleteMessage_throwsBusinessRule_whenMessageIdIsBlank(String blankId) {
        MessageAccessContext context = workspaceContext();
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_206.getCode()))
                .thenReturn("Id de mensaje requerido");

        assertThatThrownBy(() -> useCase.deleteMessage(blankId, context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Id de mensaje requerido"));

        verifyNoInteractions(environmentResolver, messageRepository, deleteMessageRepository);
    }

    @Test
    void deleteMessage_throwsBusinessRule_whenMessageNotFound() {
        MessageAccessContext context = workspaceContext();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");
        when(messageRepository.findByCodeAndApplication(eq(MESSAGE_CODE), any())).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_208.getCode()))
                .thenReturn("Mensaje no encontrado");

        assertThatThrownBy(() -> useCase.deleteMessage(MESSAGE_CODE, context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Mensaje no encontrado"));

        verifyNoInteractions(deleteMessageRepository);
    }

    @Test
    void deleteMessage_propagatesForbiddenFromEnvironmentResolver() {
        MessageAccessContext context = authenticatedContext();
        ForbiddenException failure = ForbiddenException.buildUserException("Permission denied");
        doThrow(failure).when(environmentResolver).resolveApplication(context, PermissionCode.MESSAGE_DELETE);

        assertThatThrownBy(() -> useCase.deleteMessage(MESSAGE_CODE, context)).isSameAs(failure);

        verifyNoInteractions(messageRepository, deleteMessageRepository);
    }

    @Test
    void deleteMessage_throwsCrossWordsException_whenRepositoryFails() {
        MessageAccessContext context = workspaceContext();
        stubExistingMessage();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_208.getCode()))
                .thenReturn("Mensaje no encontrado");
        doThrow(new RuntimeException("db error")).when(deleteMessageRepository)
                .deleteMessage(MESSAGE_UUID);

        assertThatThrownBy(() -> useCase.deleteMessage(MESSAGE_CODE, context))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("Mensaje no encontrado"));

        assertAll(
                () -> verify(log).error(eq("Error deleting message in repository"),
                        any(RuntimeException.class)),
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_208.getCode())
        );
    }

    @Test
    void deleteMessage_rethrowsDomainExceptionFromRepository() {
        MessageAccessContext context = workspaceContext();
        stubExistingMessage();
        when(environmentResolver.resolveApplication(any(), eq(PermissionCode.MESSAGE_DELETE))).thenReturn("env-1");
        BusinessRuleException domain = BusinessRuleException.buildUserException("conflict");
        doThrow(domain).when(deleteMessageRepository).deleteMessage(MESSAGE_UUID);

        assertThatThrownBy(() -> useCase.deleteMessage(MESSAGE_CODE, context)).isSameAs(domain);
    }
}
