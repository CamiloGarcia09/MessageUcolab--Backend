package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.facade.message.FindPublicMessageUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.ListPublicMessagesUseCaseFacade;
import co.edu.uco.infraestructure.primaryadapters.PublicMessageController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
final class PublicMessageControllerImpl implements PublicMessageController {
    private final ListPublicMessagesUseCaseFacade listFacade;
    private final FindPublicMessageUseCaseFacade findFacade;
    private final SerializerRegistry serializers;

    PublicMessageControllerImpl(ListPublicMessagesUseCaseFacade listFacade, FindPublicMessageUseCaseFacade findFacade,
                                SerializerRegistry serializers) {
        this.listFacade = listFacade;
        this.findFacade = findFacade;
        this.serializers = serializers;
    }

    @Override
    public Mono<Void> list(String applicationId, String environmentId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> listFacade.execute(applicationId, environmentId))
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }

    @Override
    public Mono<Void> find(String code, String language, String applicationId, String environmentId,
                           ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> findFacade.execute(applicationId, environmentId, code, language))
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }
}
