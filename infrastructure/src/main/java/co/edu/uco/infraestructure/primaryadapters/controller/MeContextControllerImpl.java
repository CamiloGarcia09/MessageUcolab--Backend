package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.context.*;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class MeContextControllerImpl implements MeContextController {
    private final ActiveContextUseCaseFacade facade;
    private final PresenterPort<AvailableContextDTO> available;
    private final PresenterPort<ActiveContextDTO> active;
    MeContextControllerImpl(ActiveContextUseCaseFacade facade, PresenterPort<AvailableContextDTO> available,
                            PresenterPort<ActiveContextDTO> active) {
        this.facade = facade; this.available = available; this.active = active;
    }
    public Mono<Void> getAvailableContexts(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findAvailableContexts(identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(available, value, exchange));
    }
    public Mono<Void> getActiveContext(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findActiveContext(identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(active, List.of(value), exchange));
    }
    public Mono<Void> selectActiveContext(SelectActiveContextDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.selectActiveContext(dto, identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(active, List.of(value), exchange));
    }
    private ExternalIdentity identity(ServerWebExchange exchange) {
        return (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE);
    }
}
