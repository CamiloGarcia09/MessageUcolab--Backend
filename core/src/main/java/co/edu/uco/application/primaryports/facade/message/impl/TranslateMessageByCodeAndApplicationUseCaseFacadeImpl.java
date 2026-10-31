package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingTranslateMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public final class TranslateMessageByCodeAndApplicationUseCaseFacadeImpl
        implements TranslateMessageByCodeAndApplicationUseCaseFacade {
    private final HandlingTranslateMessageByCodeAndApplicationPort handlingTranslateMessageByCodeAndApplicationPort;

    public TranslateMessageByCodeAndApplicationUseCaseFacadeImpl(
            HandlingTranslateMessageByCodeAndApplicationPort handlingTranslateMessageByCodeAndApplicationPort
    ) {
        this.handlingTranslateMessageByCodeAndApplicationPort = handlingTranslateMessageByCodeAndApplicationPort;
    }

    @Override
    public TranslatedMessageDTO execute(
            String messageCode,
            MessageAccessContext context,
            String sourceLanguage,
            String targetLanguage
    ) {
        return handlingTranslateMessageByCodeAndApplicationPort.execute(
                messageCode,
                context,
                sourceLanguage,
                targetLanguage
        );
    }
}
