package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface CreateMessageUseCaseFacade {
    void execute(CreateMessageDTO createMessageDTO, MessageAccessContext context);
}
