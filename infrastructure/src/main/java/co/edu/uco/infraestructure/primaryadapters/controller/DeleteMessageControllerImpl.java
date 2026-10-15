package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.facade.message.DeleteMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.DeleteMessageController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class DeleteMessageControllerImpl implements DeleteMessageController {

    private final DeleteMessageUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    private final CatalogPort catalogPort;

    DeleteMessageControllerImpl(DeleteMessageUseCaseFacade facade,
                                PresenterPort<String> presenter,
                                CatalogPort catalogPort) {
        this.facade = facade;
        this.presenter = presenter;
        this.catalogPort = catalogPort;
    }

    @Override
    public Mono<Void> deleteMessage(String messageId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.execute(messageId, context(exchange));
            return catalogPort.getMessage(MessageCatalogCodeEnum.FUN_210.getCode());
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }

    private MessageAccessContext context(ServerWebExchange exchange) {
        return new MessageAccessContext(
                (String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE),
                (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE));
    }
}
