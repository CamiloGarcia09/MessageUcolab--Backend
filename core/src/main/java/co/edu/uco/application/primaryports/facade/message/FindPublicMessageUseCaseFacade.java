package co.edu.uco.application.primaryports.facade.message;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;

public interface FindPublicMessageUseCaseFacade {

    /** Sin idioma se entrega el texto original, en el idioma base de la aplicacion. */
    ConsumerTranslatedMessageDTO execute(String applicationId, String environmentId, String code, String language);
}
