package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingUpdateMessagePort {
    void updateMessage(String messageId, UpdateMessageDTO dto, MessageAccessContext context);
}
