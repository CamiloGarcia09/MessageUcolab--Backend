package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.MESSAGE_CODE_PARAMETER;

@RequestMapping("${crosswords.api.path.messages}")
public interface UpdateMessageController {

    @PutMapping(
            value = "/{" + MESSAGE_CODE_PARAMETER + "}",
            consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> updateMessage(
            @PathVariable(MESSAGE_CODE_PARAMETER) String messageId,
            @RequestBody UpdateMessageDTO dto,
            ServerWebExchange exchange
    );
}
