package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingCreateMessagePort {
    void createMessage(CreateMessageDTO messageDTO, MessageAccessContext context);
}
