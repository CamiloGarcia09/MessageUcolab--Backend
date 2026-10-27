package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslationRequestDTO;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.ConsumerMessageTranslationController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;

@RestController
final class ConsumerMessageTranslationControllerImpl implements ConsumerMessageTranslationController {
    private final TranslateConsumerMessageUseCaseFacade facade;
    private final SerializerRegistry serializers;

    ConsumerMessageTranslationControllerImpl(TranslateConsumerMessageUseCaseFacade facade, SerializerRegistry serializers) {
        this.facade = facade;
        this.serializers = serializers;
    }

    @Override
    public Mono<Void> translate(String code, ConsumerTranslationRequestDTO request, ServerWebExchange exchange) {
        var context = new MessageAccessContext(exchange.getAttribute(APPLICATION_ID_ATTRIBUTE));
        String environmentId = exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE);
        return ReactiveHttpResponse.blocking(() -> facade.execute(code, environmentId, context, request.getTargetLanguage()))
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }
}
