package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteMessageUseCaseFacadeImplTest {

    @Mock
    private HandlingDeleteMessagePort handlingDeleteMessagePort;

    @InjectMocks
    private DeleteMessageUseCaseFacadeImpl facade;

    @Test
    void execute_delegatesWithLegacyEnvironmentContext() {
        String messageId = "MSG_WELCOME";
        MessageAccessContext context = new MessageAccessContext("env-1", null);

        facade.execute(messageId, context);

        verify(handlingDeleteMessagePort).deleteMessage(messageId, context);
    }

    @Test
    void execute_delegatesWithAuthenticatedIdentityContext() {
        String messageId = "MSG_HELLO";
        ExternalIdentity identity = new ExternalIdentity(
                "issuer", "subject", "user@example.com", Instant.MAX);
        MessageAccessContext context = new MessageAccessContext(null, identity);

        facade.execute(messageId, context);

        verify(handlingDeleteMessagePort).deleteMessage(messageId, context);
    }
}
