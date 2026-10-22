package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.application.CreateApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

@RestController
final class CreateApplicationControllerImpl implements CreateApplicationController {
    private final CreateApplicationUseCaseFacade facade;
    private final SessionApplicationUseCaseFacade sessionFacade;
    private final PresenterPort<String> presenter;
    CreateApplicationControllerImpl(CreateApplicationUseCaseFacade facade, SessionApplicationUseCaseFacade sessionFacade,
                                    PresenterPort<String> presenter) {
        this.facade = facade; this.sessionFacade = sessionFacade; this.presenter = presenter;
    }
    public Mono<Void> createApplication(CreateApplicationDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
                    authorizeSessionApplication(dto, exchange);
                     facade.execute(dto);
                    return "Aplicación creada exitosamente";
                })
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
    // Solo se registra una aplicacion asignada al usuario de la sesion, con su mismo UUID.
    @SuppressWarnings("unchecked")
    private void authorizeSessionApplication(CreateApplicationDTO dto, ServerWebExchange exchange) {
        var sessionApplications = (List<CatalogItemDTO>) exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE);
        sessionFacade.authorizeApplication(sessionApplications, dto == null ? null : dto.getId());
    }
}
