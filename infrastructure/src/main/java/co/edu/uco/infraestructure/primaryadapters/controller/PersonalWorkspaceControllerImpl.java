package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.infraestructure.primaryadapters.PersonalWorkspaceController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;

@RestController
public final class PersonalWorkspaceControllerImpl implements PersonalWorkspaceController {
    private final PersonalWorkspaceUseCaseFacade workspace;
    private final PresenterPort<CatalogItemDTO> itemsPresenter;
    private final PresenterPort<WorkspaceUserDTO> userPresenter;

    public PersonalWorkspaceControllerImpl(PersonalWorkspaceUseCaseFacade workspace,
                                            PresenterPort<CatalogItemDTO> itemsPresenter,
                                            PresenterPort<WorkspaceUserDTO> userPresenter) {
        this.workspace = workspace;
        this.itemsPresenter = itemsPresenter;
        this.userPresenter = userPresenter;
    }

    @Override
    public Mono<Void> currentUser(ServerWebExchange exchange) {
        return ReactiveHttpResponse.present(userPresenter, List.of(user(exchange)), exchange);
    }

    @Override
    public Mono<Void> organizations(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> workspace.findOrganizations(user(exchange)))
                .flatMap(items -> ReactiveHttpResponse.present(itemsPresenter, items, exchange));
    }

    @Override
    public Mono<Void> createOrganization(CreateOrganizationDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> workspace.createOrganization(dto, user(exchange)))
                .flatMap(item -> ReactiveHttpResponse.present(itemsPresenter, List.of(item), exchange));
    }

    @Override
    public Mono<Void> applications(String organizationId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> workspace.findApplications(organizationId, user(exchange)))
                .flatMap(items -> ReactiveHttpResponse.present(itemsPresenter, items, exchange));
    }

    private WorkspaceUserDTO user(ServerWebExchange exchange) {
        return exchange.getAttribute(WORKSPACE_USER_ATTRIBUTE);
    }
}
