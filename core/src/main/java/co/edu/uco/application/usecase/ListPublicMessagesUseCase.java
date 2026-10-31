package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.PublicMessageListDTO;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ListPublicMessagesUseCaseFacade;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.security.PublicMessageAccessResolver;

import java.util.Locale;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class ListPublicMessagesUseCase implements ListPublicMessagesUseCaseFacade {

    private final PublicMessageAccessResolver accessResolver;
    private final ResolveMessageRepository repository;
    private final ApplicationLanguageRepository languages;

    public ListPublicMessagesUseCase(PublicMessageAccessResolver accessResolver, ResolveMessageRepository repository,
                                     ApplicationLanguageRepository languages) {
        this.accessResolver = accessResolver;
        this.repository = repository;
        this.languages = languages;
    }

    @Override
    public PublicMessageListDTO execute(String applicationId, String environmentId) {
        var access = accessResolver.resolve(applicationId, environmentId);
        // Mismas condiciones que la consulta de un mensaje: activo y con asociacion activa al ambiente.
        var messages = repository.findAvailableByEnvironment(access.environmentId()).stream()
                .map(ListPublicMessagesUseCase::toResolvedMessage)
                .toList();
        var language = trim(languages.findBaseLanguageCode(access.applicationId()).orElse(""))
                .replace('_', '-').toLowerCase(Locale.ROOT);
        return new PublicMessageListDTO(language, messages);
    }

    private static ResolvedMessageDTO toResolvedMessage(MessageData message) {
        return new ResolvedMessageDTO(message.getCode(), message.getTitle(), message.getContent(),
                message.getType().getName(), message.getCategory().getName(),
                message.getFunctionality().getName(), message.getCreatedAt(), message.getUpdatedAt());
    }
}
