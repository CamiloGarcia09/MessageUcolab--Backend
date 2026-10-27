package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingCreateMessagePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateMessageUseCaseFacadeImplTest {

    @Mock
    private HandlingCreateMessagePort handlingCreateMessagePort;

    @InjectMocks
    private CreateMessageUseCaseFacadeImpl facade;

    @Test
    void execute_delegatesMessageAndLegacyEnvironmentContext() {
        CreateMessageDTO dto = new CreateMessageDTO();
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(dto, context);

        verify(handlingCreateMessagePort).createMessage(dto, context);
    }

    @Test
    void execute_delegatesMessageAndSessionApplicationContext() {
        CreateMessageDTO dto = new CreateMessageDTO();
        MessageAccessContext context = new MessageAccessContext("app-1");

        facade.execute(dto, context);

        verify(handlingCreateMessagePort).createMessage(dto, context);
    }
}
