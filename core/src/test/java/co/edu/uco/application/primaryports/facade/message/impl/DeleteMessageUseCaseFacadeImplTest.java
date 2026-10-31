package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


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
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(messageId, context);

        verify(handlingDeleteMessagePort).deleteMessage(messageId, context);
    }

    @Test
    void execute_delegatesWithAuthenticatedIdentityContext() {
        String messageId = "MSG_HELLO";
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(messageId, context);

        verify(handlingDeleteMessagePort).deleteMessage(messageId, context);
    }
}
