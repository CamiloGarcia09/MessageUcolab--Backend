package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.primaryadapters.SessionApplicationController;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

@RestController
final class SessionApplicationControllerImpl implements SessionApplicationController {
    private final SessionApplicationUseCaseFacade facade;
    private final PresenterPort<SessionApplicationDTO> presenter;
    SessionApplicationControllerImpl(SessionApplicationUseCaseFacade facade,
                                     PresenterPort<SessionApplicationDTO> presenter) {
        this.facade = facade; this.presenter = presenter;
    }
    public Mono<Void> getSessionApplications(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findSessionApplications(sessionApplications(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    @SuppressWarnings("unchecked")
    private List<CatalogItemDTO> sessionApplications(ServerWebExchange exchange) {
        return (List<CatalogItemDTO>) exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE);
    }
}
