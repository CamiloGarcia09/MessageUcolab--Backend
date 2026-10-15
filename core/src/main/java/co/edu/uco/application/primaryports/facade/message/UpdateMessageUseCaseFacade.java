package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface UpdateMessageUseCaseFacade {
    void execute(String messageId, UpdateMessageDTO dto, MessageAccessContext context);
}
