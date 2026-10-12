package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.CreateMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.event.*;
import co.edu.uco.application.usecase.domain.security.*;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@RestController
final class CreateMessageControllerImpl implements CreateMessageController {
    private final CreateMessageUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    private final Publisher<MessageEvent> publisher;
    CreateMessageControllerImpl(CreateMessageUseCaseFacade facade, PresenterPort<String> presenter,
                                Publisher<MessageEvent> publisher) {
        this.facade = facade; this.presenter = presenter; this.publisher = publisher;
    }
    public Mono<Void> createMessage(CreateMessageDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.execute(dto, new MessageAccessContext((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE),
                    (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)));
            publisher.publish(new MessageEvent(UUID.randomUUID().toString(), "MessageCreated",
                    (String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE), dto.getCode()));
            return "Mensaje creado exitosamente";
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
