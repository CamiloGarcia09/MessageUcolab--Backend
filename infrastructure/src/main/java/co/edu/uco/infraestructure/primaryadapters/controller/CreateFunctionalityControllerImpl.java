package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.primaryports.facade.functionality.CreateFunctionalityUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class CreateFunctionalityControllerImpl implements CreateFunctionalityController {
    private final CreateFunctionalityUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    CreateFunctionalityControllerImpl(CreateFunctionalityUseCaseFacade facade, PresenterPort<String> presenter) {
        this.facade = facade; this.presenter = presenter;
    }
    public Mono<Void> createFunctionality(CreateFunctionalityDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> { facade.execute(dto, identity(exchange)); return "Funcionalidad creada exitosamente"; })
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
    private ExternalIdentity identity(ServerWebExchange exchange) {
        return (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE);
    }
}
