package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.infraestructure.primaryadapters.CatalogController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class CatalogControllerImpl implements CatalogController {
    private final FindCatalogUseCaseFacade facade;
    private final PresenterPort<CatalogItemDTO> presenter;

    CatalogControllerImpl(FindCatalogUseCaseFacade facade, PresenterPort<CatalogItemDTO> presenter) {
        this.facade = facade;
        this.presenter = presenter;
    }

    public Mono<Void> getApplications(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findApplications(identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getEnvironmentsByApplication(String id, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findEnvironmentsByApplication(id, identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getFunctionalitiesByApplication(String id, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.findFunctionalitiesByApplication(id, identity(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getMessageTypes(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(facade::findMessageTypes)
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getMessageCategories(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(facade::findMessageCategories)
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getMessageStates(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(facade::findMessageStates)
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    public Mono<Void> getMessageEnvironmentStates(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(facade::findMessageEnvironmentStates)
                .flatMap(value -> ReactiveHttpResponse.present(presenter, value, exchange));
    }
    private ExternalIdentity identity(ServerWebExchange exchange) {
        return (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE);
    }
}
