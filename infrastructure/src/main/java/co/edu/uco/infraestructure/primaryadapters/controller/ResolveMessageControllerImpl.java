package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.facade.message.ListConsumerMessagesUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.primaryadapters.consumer.ConsumerRequestAccess;
import co.edu.uco.infraestructure.primaryadapters.ResolveMessageController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
final class ResolveMessageControllerImpl implements ResolveMessageController {

    private final ResolveMessageUseCaseFacade facade;
    private final ListConsumerMessagesUseCaseFacade listFacade;
    private final SerializerRegistry serializers;
    private final ConsumerRequestAccess consumerAccess;

    ResolveMessageControllerImpl(ResolveMessageUseCaseFacade facade, ListConsumerMessagesUseCaseFacade listFacade,
                                 SerializerRegistry serializers, ConsumerRequestAccess consumerAccess) {
        this.facade = facade;
        this.listFacade = listFacade;
        this.serializers = serializers;
        this.consumerAccess = consumerAccess;
    }

    /**
     * Con {@code code} entrega ese mensaje; sin el, todos los mensajes disponibles del ambiente.
     */
    public Mono<Void> resolve(String code, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
                    var access = consumerAccess.resolve(exchange);
                    return code == null
                            ? listFacade.execute(access.environmentId(), access.context())
                            : facade.execute(code, access.environmentId(), access.context());
                })
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }
}
