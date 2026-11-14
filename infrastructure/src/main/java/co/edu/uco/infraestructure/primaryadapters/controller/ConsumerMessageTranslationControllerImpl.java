package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslationRequestDTO;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.infraestructure.primaryadapters.consumer.ConsumerRequestAccess;
import co.edu.uco.infraestructure.primaryadapters.ConsumerMessageTranslationController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
final class ConsumerMessageTranslationControllerImpl implements ConsumerMessageTranslationController {
    private final TranslateConsumerMessageUseCaseFacade facade;
    private final SerializerRegistry serializers;
    private final ConsumerRequestAccess consumerAccess;

    ConsumerMessageTranslationControllerImpl(TranslateConsumerMessageUseCaseFacade facade, SerializerRegistry serializers,
                                             ConsumerRequestAccess consumerAccess) {
        this.facade = facade;
        this.serializers = serializers;
        this.consumerAccess = consumerAccess;
    }

    @Override
    public Mono<Void> translate(String code, ConsumerTranslationRequestDTO request, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
                    var access = consumerAccess.resolve(exchange);
                    return facade.execute(code, access.environmentId(), access.context(), request.getTargetLanguage());
                })
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }
}
