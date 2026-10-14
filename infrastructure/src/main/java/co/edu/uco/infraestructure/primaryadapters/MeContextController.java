package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.context.SelectActiveContextDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/messageucolab/v1/me")
public interface MeContextController {

    @GetMapping(value = "/contexts", produces = "application/json")
    Mono<Void> getAvailableContexts(ServerWebExchange exchange);

    @GetMapping(value = "/context", produces = "application/json")
    Mono<Void> getActiveContext(ServerWebExchange exchange);

    @PutMapping(value = "/context", consumes = "application/json", produces = "application/json")
    Mono<Void> selectActiveContext(@RequestBody SelectActiveContextDTO context,
                             ServerWebExchange exchange);
}
