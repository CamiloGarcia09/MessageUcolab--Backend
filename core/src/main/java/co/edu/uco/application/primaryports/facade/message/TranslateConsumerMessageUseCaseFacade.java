package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface TranslateConsumerMessageUseCaseFacade {
    ConsumerTranslatedMessageDTO execute(String code, String environmentId, MessageAccessContext context,
                                          String targetLanguage);
}
