package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface DeleteMessageUseCaseFacade {
    void execute(String messageId, MessageAccessContext context);
}
