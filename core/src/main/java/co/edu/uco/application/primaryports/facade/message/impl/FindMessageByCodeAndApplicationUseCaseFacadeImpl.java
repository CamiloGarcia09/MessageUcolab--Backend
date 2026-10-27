package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingFindMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public final class FindMessageByCodeAndApplicationUseCaseFacadeImpl implements FindMessageByCodeAndApplicationUseCaseFacade {
    private final HandlingFindMessageByCodeAndApplicationPort handlingFindMessageByCodeAndApplicationPort;
    public FindMessageByCodeAndApplicationUseCaseFacadeImpl(
            HandlingFindMessageByCodeAndApplicationPort handlingFindMessageByCodeAndApplicationPort) {
        this.handlingFindMessageByCodeAndApplicationPort = handlingFindMessageByCodeAndApplicationPort;
    }
    @Override
    public MessageDTO execute(String messageCode, MessageAccessContext context) {
        return handlingFindMessageByCodeAndApplicationPort.execute(messageCode, context);
    }
}
