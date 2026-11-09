package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.application.DeleteApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.DeleteApplicationController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_230;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;

@RestController
final class DeleteApplicationControllerImpl implements DeleteApplicationController {
    private final DeleteApplicationUseCaseFacade facade;
    private final CatalogPort catalog;
    private final PresenterPort<String> presenter;

    DeleteApplicationControllerImpl(DeleteApplicationUseCaseFacade facade, CatalogPort catalog,
            PresenterPort<String> presenter) {
        this.facade = facade;
        this.catalog = catalog;
        this.presenter = presenter;
    }

    @Override
    public Mono<Void> deleteApplication(String applicationId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            WorkspaceUserDTO user = exchange.getAttribute(WORKSPACE_USER_ATTRIBUTE);
            // Resolve the response before deleting: a missing catalog entry must not mask a successful commit.
            String confirmation = catalog.getMessage(FUN_230.getCode());
            facade.execute(applicationId, user);
            return confirmation;
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
