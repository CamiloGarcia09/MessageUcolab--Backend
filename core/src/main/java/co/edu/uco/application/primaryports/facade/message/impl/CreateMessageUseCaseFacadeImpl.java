package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.CreateMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingCreateMessagePort;
import org.springframework.stereotype.Component;

@Component
public final class CreateMessageUseCaseFacadeImpl implements CreateMessageUseCaseFacade {

    private final HandlingCreateMessagePort handlingCreateMessagePort;

    public CreateMessageUseCaseFacadeImpl(HandlingCreateMessagePort handlingCreateMessagePort) {
        this.handlingCreateMessagePort = handlingCreateMessagePort;
    }

    @Override
    public void execute(CreateMessageDTO createMessageDTO, MessageAccessContext context) {
        handlingCreateMessagePort.createMessage(createMessageDTO, context);
    }
}
