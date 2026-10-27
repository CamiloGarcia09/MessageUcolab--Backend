package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingUpdateMessagePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateMessageUseCaseFacadeImplTest {

    @Mock
    private HandlingUpdateMessagePort handlingUpdateMessagePort;

    @InjectMocks
    private UpdateMessageUseCaseFacadeImpl facade;

    @Test
    void execute_delegatesWithLegacyEnvironmentContext() {
        String messageId = "MSG_WELCOME";
        UpdateMessageDTO dto = new UpdateMessageDTO();
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(messageId, dto, context);

        verify(handlingUpdateMessagePort).updateMessage(messageId, dto, context);
    }

    @Test
    void execute_delegatesWithAuthenticatedIdentityContext() {
        String messageId = "MSG_HELLO";
        UpdateMessageDTO dto = new UpdateMessageDTO();
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(messageId, dto, context);

        verify(handlingUpdateMessagePort).updateMessage(messageId, dto, context);
    }
}
