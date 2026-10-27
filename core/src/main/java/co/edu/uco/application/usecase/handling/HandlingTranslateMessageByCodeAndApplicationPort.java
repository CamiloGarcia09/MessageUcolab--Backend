package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingTranslateMessageByCodeAndApplicationPort {
    TranslatedMessageDTO execute(String messageCode, MessageAccessContext context,
                                 String sourceLanguage, String targetLanguage);
}
