package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface ResolveMessageUseCaseFacade {

    ResolvedMessageDTO execute(String code, String environmentId, MessageAccessContext context);
}
