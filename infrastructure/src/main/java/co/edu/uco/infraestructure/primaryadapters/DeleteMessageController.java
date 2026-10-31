package co.edu.uco.infraestructure.primaryadapters;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.MESSAGE_CODE_PARAMETER;

@RequestMapping("${crosswords.api.path.messages}")
public interface DeleteMessageController {

    @DeleteMapping(
            value = "/{" + MESSAGE_CODE_PARAMETER + "}",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> deleteMessage(
            @PathVariable(MESSAGE_CODE_PARAMETER) String messageId,
            ServerWebExchange exchange
    );
}
