package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.PublicMessageListDTO;

public interface ListPublicMessagesUseCaseFacade {

    PublicMessageListDTO execute(String applicationId, String environmentId);
}
