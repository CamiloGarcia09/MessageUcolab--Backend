package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.ResolveMessageController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@RestController
final class ResolveMessageControllerImpl implements ResolveMessageController {
    private final ResolveMessageUseCaseFacade facade;
    private final SerializerRegistry serializers;
    ResolveMessageControllerImpl(ResolveMessageUseCaseFacade facade, SerializerRegistry serializers) {
        this.facade = facade;
        this.serializers = serializers;
    }
    public Mono<Void> resolve(String code, ServerWebExchange exchange) {
        var context = new MessageAccessContext(
                (String) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE));
        String environmentId = exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE);
        return ReactiveHttpResponse.blocking(() -> facade.execute(code, environmentId, context))
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }
}
