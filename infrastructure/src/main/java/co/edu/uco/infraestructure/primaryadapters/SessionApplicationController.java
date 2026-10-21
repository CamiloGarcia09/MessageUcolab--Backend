package co.edu.uco.infraestructure.primaryadapters;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("/messageucolab/v1/session")
public interface SessionApplicationController {

    @GetMapping(value = "/applications", produces = "application/json")
    Mono<Void> getSessionApplications(ServerWebExchange exchange);
}
