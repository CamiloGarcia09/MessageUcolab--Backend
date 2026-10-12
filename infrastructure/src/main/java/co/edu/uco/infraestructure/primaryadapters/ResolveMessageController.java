package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("${crosswords.api.path.messages}")
public interface ResolveMessageController {

    @GetMapping(value = "/resolve", produces = "application/json")
    Mono<Void> resolve(@RequestParam("code") String code,
                       ServerWebExchange exchange);
}
