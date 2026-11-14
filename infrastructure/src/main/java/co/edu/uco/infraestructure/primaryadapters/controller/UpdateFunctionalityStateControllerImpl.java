package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import co.edu.uco.application.primaryports.facade.functionality.UpdateFunctionalityStateUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.primaryadapters.UpdateFunctionalityStateController;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_227;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

@RestController
final class UpdateFunctionalityStateControllerImpl implements UpdateFunctionalityStateController {
    private final UpdateFunctionalityStateUseCaseFacade facade;
    private final SessionApplicationUseCaseFacade session;
    private final PresenterPort<String> presenter;
    private final CatalogPort catalog;

    UpdateFunctionalityStateControllerImpl(UpdateFunctionalityStateUseCaseFacade facade,
            SessionApplicationUseCaseFacade session, PresenterPort<String> presenter, CatalogPort catalog) {
        this.facade = facade;
        this.session = session;
        this.presenter = presenter;
        this.catalog = catalog;
    }

    @Override
    public Mono<Void> updateState(String functionalityId, UpdateFunctionalityStateDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            List<CatalogItemDTO> applications = exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE);
            String applicationId = session.resolveApplication(applications, dto == null ? null : dto.applicationId());
            facade.execute(functionalityId, dto == null ? null : new UpdateFunctionalityStateDTO(applicationId, dto.stateId()));
            return catalog.getMessage(FUN_227.getCode());
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
