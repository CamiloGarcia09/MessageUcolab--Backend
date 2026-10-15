package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.facade.message.DeleteMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import org.springframework.stereotype.Component;

@Component
public final class DeleteMessageUseCaseFacadeImpl implements DeleteMessageUseCaseFacade {

    private final HandlingDeleteMessagePort handlingDeleteMessagePort;

    public DeleteMessageUseCaseFacadeImpl(HandlingDeleteMessagePort handlingDeleteMessagePort) {
        this.handlingDeleteMessagePort = handlingDeleteMessagePort;
    }

    @Override
    public void execute(String messageId, MessageAccessContext context) {
        handlingDeleteMessagePort.deleteMessage(messageId, context);
    }
}
