package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;

@RestController
final class CreateApplicationControllerImpl implements CreateApplicationController {
    private final PersonalWorkspaceUseCaseFacade workspace;
    private final CatalogPort catalog;
    private final PresenterPort<String> presenter;
    CreateApplicationControllerImpl(PersonalWorkspaceUseCaseFacade workspace, CatalogPort catalog,
                                    PresenterPort<String> presenter) {
        this.workspace = workspace; this.catalog = catalog; this.presenter = presenter;
    }
    public Mono<Void> createApplication(CreateApplicationDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
                    workspace.createApplication(dto, exchange.<WorkspaceUserDTO>getAttribute(WORKSPACE_USER_ATTRIBUTE));
                    return catalog.getMessage(MessageCatalogCodeEnum.FUN_216.getCode());
                })
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
