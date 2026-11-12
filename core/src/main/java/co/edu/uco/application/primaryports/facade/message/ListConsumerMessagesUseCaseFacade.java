package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.ConsumerMessageListDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface ListConsumerMessagesUseCaseFacade {

    ConsumerMessageListDTO execute(String environmentId, MessageAccessContext context);
}
