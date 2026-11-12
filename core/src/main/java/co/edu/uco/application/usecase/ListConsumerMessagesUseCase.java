package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.ConsumerMessageListDTO;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ListConsumerMessagesUseCaseFacade;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;

import java.util.Locale;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

/**
 * Mensajes disponibles de la aplicacion consumidora en un ambiente, en el idioma base de la aplicacion.
 * Aplica las mismas condiciones que la consulta de un mensaje: activo y con asociacion activa al ambiente.
 */
public final class ListConsumerMessagesUseCase implements ListConsumerMessagesUseCaseFacade {

    private final MessageEnvironmentResolver environmentResolver;
    private final ResolveMessageRepository repository;
    private final ApplicationLanguageRepository languages;

    public ListConsumerMessagesUseCase(MessageEnvironmentResolver environmentResolver,
                                       ResolveMessageRepository repository,
                                       ApplicationLanguageRepository languages) {
        this.environmentResolver = environmentResolver;
        this.repository = repository;
        this.languages = languages;
    }

    @Override
    public ConsumerMessageListDTO execute(String environmentId, MessageAccessContext context) {
        var environment = environmentResolver.resolve(context, environmentId);
        var messages = repository.findAvailableByEnvironment(environment).stream()
                .map(ListConsumerMessagesUseCase::toResolvedMessage)
                .toList();
        var language = trim(languages.findBaseLanguageCode(context.applicationId()).orElse(""))
                .replace('_', '-').toLowerCase(Locale.ROOT);
        return new ConsumerMessageListDTO(language, messages);
    }

    private static ResolvedMessageDTO toResolvedMessage(MessageData message) {
        return new ResolvedMessageDTO(message.getCode(), message.getTitle(), message.getContent(),
                message.getType().getName(), message.getCategory().getName(),
                message.getFunctionality().getName(), message.getCreatedAt(), message.getUpdatedAt());
    }
}
