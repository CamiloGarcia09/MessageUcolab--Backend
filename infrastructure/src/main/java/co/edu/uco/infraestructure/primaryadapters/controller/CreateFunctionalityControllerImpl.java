package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.functionality.CreateFunctionalityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

@RestController
final class CreateFunctionalityControllerImpl implements CreateFunctionalityController {
    private final CreateFunctionalityUseCaseFacade facade;
    private final SessionApplicationUseCaseFacade sessionFacade;
    private final PresenterPort<String> presenter;
    CreateFunctionalityControllerImpl(CreateFunctionalityUseCaseFacade facade,
                                      SessionApplicationUseCaseFacade sessionFacade, PresenterPort<String> presenter) {
        this.facade = facade; this.sessionFacade = sessionFacade; this.presenter = presenter;
    }
    public Mono<Void> createFunctionality(CreateFunctionalityDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
                    sessionFacade.resolveApplication(sessionApplications(exchange), dto == null ? null : dto.getApplicationId());
                    facade.execute(dto);
                    return "Funcionalidad creada exitosamente";
                })
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
    private List<CatalogItemDTO> sessionApplications(ServerWebExchange exchange) {
        return exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE);
    }
}
