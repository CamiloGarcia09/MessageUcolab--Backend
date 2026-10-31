package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.UpdateMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.primaryadapters.UpdateMessageController;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;

@RestController
final class UpdateMessageControllerImpl implements UpdateMessageController {

    private final UpdateMessageUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    private final CatalogPort catalogPort;

    UpdateMessageControllerImpl(UpdateMessageUseCaseFacade facade,
                                PresenterPort<String> presenter,
                                CatalogPort catalogPort) {
        this.facade = facade;
        this.presenter = presenter;
        this.catalogPort = catalogPort;
    }

    @Override
    public Mono<Void> updateMessage(String messageId, UpdateMessageDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.execute(messageId, dto, context(exchange));
            return catalogPort.getMessage(MessageCatalogCodeEnum.FUN_209.getCode());
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }

    private MessageAccessContext context(ServerWebExchange exchange) {
        return new MessageAccessContext(
                (String) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE));
    }
}
