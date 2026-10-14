package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.token.CreateTokenDTO;
import co.edu.uco.application.primaryports.facade.token.CreateTokenUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class TokenControllerImpl implements TokenController {
    private final CreateTokenUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    TokenControllerImpl(CreateTokenUseCaseFacade facade, PresenterPort<String> presenter) {
        this.facade = facade; this.presenter = presenter;
    }
    public Mono<Void> createToken(CreateTokenDTO dto, String applicationId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            var result = facade.execute(dto, applicationId);
            return String.valueOf(result);
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
