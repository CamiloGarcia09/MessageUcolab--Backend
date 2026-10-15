package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.UpdateMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingUpdateMessagePort;
import org.springframework.stereotype.Component;

@Component
public final class UpdateMessageUseCaseFacadeImpl implements UpdateMessageUseCaseFacade {

    private final HandlingUpdateMessagePort handlingUpdateMessagePort;

    public UpdateMessageUseCaseFacadeImpl(HandlingUpdateMessagePort handlingUpdateMessagePort) {
        this.handlingUpdateMessagePort = handlingUpdateMessagePort;
    }

    @Override
    public void execute(String messageId, UpdateMessageDTO dto, MessageAccessContext context) {
        handlingUpdateMessagePort.updateMessage(messageId, dto, context);
    }
}
