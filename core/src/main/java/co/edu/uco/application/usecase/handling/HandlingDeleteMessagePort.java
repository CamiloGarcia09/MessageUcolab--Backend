package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingDeleteMessagePort {
    void deleteMessage(String messageId, MessageAccessContext context);
}
