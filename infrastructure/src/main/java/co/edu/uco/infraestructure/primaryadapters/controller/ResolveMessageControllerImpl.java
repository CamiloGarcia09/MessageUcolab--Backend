package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.*;
import co.edu.uco.infraestructure.primaryadapters.ResolveMessageController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@RestController
final class ResolveMessageControllerImpl implements ResolveMessageController {
    private final ResolveMessageUseCaseFacade facade;
    private final PresenterPort<ResolvedMessageDTO> presenter;
    ResolveMessageControllerImpl(ResolveMessageUseCaseFacade facade, PresenterPort<ResolvedMessageDTO> presenter) {
        this.facade = facade; this.presenter = presenter;
    }
    public Mono<Void> resolve(String code, ServerWebExchange exchange) {
        var context = new MessageAccessContext((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE),
                (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE));
        return ReactiveHttpResponse.blocking(() -> facade.execute(code, context))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
