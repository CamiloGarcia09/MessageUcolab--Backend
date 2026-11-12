package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.facade.serviceidentity.ServiceIdentityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.primaryadapters.ServiceIdentityController;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_236;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_238;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

/**
 * Solo el propietario administra las identidades: la aplicacion se valida contra las de su sesion BFF.
 */
@RestController
final class ServiceIdentityControllerImpl implements ServiceIdentityController {

    private final ServiceIdentityUseCaseFacade facade;
    private final SessionApplicationUseCaseFacade session;
    private final PresenterPort<String> presenter;
    private final SerializerRegistry serializers;
    private final CatalogPort catalog;

    ServiceIdentityControllerImpl(ServiceIdentityUseCaseFacade facade, SessionApplicationUseCaseFacade session,
                                  PresenterPort<String> presenter, SerializerRegistry serializers,
                                  CatalogPort catalog) {
        this.facade = facade;
        this.session = session;
        this.presenter = presenter;
        this.serializers = serializers;
        this.catalog = catalog;
    }

    @Override
    public Mono<Void> list(String applicationId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> facade.list(ownApplication(applicationId, exchange)))
                .flatMap(value -> ReactiveHttpResponse.presentUncachedJson(serializers, value, exchange));
    }

    @Override
    public Mono<Void> register(String applicationId, RegisterServiceIdentityDTO request, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.register(ownApplication(applicationId, exchange), request);
            return catalog.getMessage(FUN_236.getCode());
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }

    @Override
    public Mono<Void> remove(String applicationId, String clientId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.remove(ownApplication(applicationId, exchange), clientId);
            return catalog.getMessage(FUN_238.getCode());
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }

    private String ownApplication(String applicationId, ServerWebExchange exchange) {
        List<CatalogItemDTO> applications = exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE);
        return session.resolveApplication(applications, applicationId);
    }
}
