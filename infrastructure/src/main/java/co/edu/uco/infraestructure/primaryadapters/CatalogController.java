package co.edu.uco.infraestructure.primaryadapters;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/messageucolab/v1/catalog")
public interface CatalogController {

    @GetMapping(
            value = "/applications",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getApplications(ServerWebExchange exchange);

    @GetMapping(
            value = "/applications/{applicationId}/environments",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getEnvironmentsByApplication(
            @PathVariable("applicationId") String applicationId,
            ServerWebExchange exchange
    );

    @GetMapping(
            value = "/applications/{applicationId}/functionalities",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getFunctionalitiesByApplication(
            @PathVariable("applicationId") String applicationId,
            ServerWebExchange exchange
    );

    @GetMapping(
            value = "/message-types",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getMessageTypes(ServerWebExchange exchange);

    @GetMapping(
            value = "/message-categories",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getMessageCategories(ServerWebExchange exchange);

    @GetMapping(
            value = "/message-states",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getMessageStates(ServerWebExchange exchange);

    @GetMapping(
            value = "/message-environment-states",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> getMessageEnvironmentStates(ServerWebExchange exchange);
}
