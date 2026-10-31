package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.CreateMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@RestController
final class CreateMessageControllerImpl implements CreateMessageController {
    private final CreateMessageUseCaseFacade facade;
    private final PresenterPort<String> presenter;
    CreateMessageControllerImpl(CreateMessageUseCaseFacade facade, PresenterPort<String> presenter) {
        this.facade = facade; this.presenter = presenter;
    }
    public Mono<Void> createMessage(CreateMessageDTO dto, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            facade.execute(dto, new MessageAccessContext(
                    (String) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE)));
            return "Mensaje creado exitosamente";
        }).flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
}
