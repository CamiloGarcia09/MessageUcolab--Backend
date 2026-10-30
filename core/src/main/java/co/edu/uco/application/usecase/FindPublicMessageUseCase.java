package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.FindPublicMessageUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.usecase.security.PublicMessageAccessResolver;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

public final class FindPublicMessageUseCase implements FindPublicMessageUseCaseFacade {

    private final PublicMessageAccessResolver accessResolver;
    private final TranslateConsumerMessageUseCaseFacade translator;
    private final ApplicationLanguageRepository languages;

    public FindPublicMessageUseCase(PublicMessageAccessResolver accessResolver,
                                    TranslateConsumerMessageUseCaseFacade translator,
                                    ApplicationLanguageRepository languages) {
        this.accessResolver = accessResolver;
        this.translator = translator;
        this.languages = languages;
    }

    @Override
    public ConsumerTranslatedMessageDTO execute(String applicationId, String environmentId, String code,
                                                String language) {
        var access = accessResolver.resolve(applicationId, environmentId);
        // En el idioma base la estrategia devuelve el texto original, sin llamar al traductor.
        var target = isEmptyOrNull(language)
                ? languages.findBaseLanguageCode(access.applicationId()).orElse("")
                : language;
        return translator.execute(code, access.environmentId(), access.context(), target);
    }
}
