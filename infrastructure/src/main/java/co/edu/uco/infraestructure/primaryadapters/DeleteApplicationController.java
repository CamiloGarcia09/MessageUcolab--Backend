package co.edu.uco.infraestructure.primaryadapters;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("${crosswords.api.path.application}")
public interface DeleteApplicationController {
    @DeleteMapping(value = "/{applicationId}", produces = "application/json")
    Mono<Void> deleteApplication(@PathVariable("applicationId") String applicationId, ServerWebExchange exchange);
}
