package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Identidades de servicio que pueden consumir los mensajes de cada ambiente de una aplicacion propia.
 */
@RequestMapping("${crosswords.api.path.application}/{applicationId}/service-identities")
public interface ServiceIdentityController {

    @GetMapping(produces = "application/json")
    Mono<Void> list(@PathVariable("applicationId") String applicationId, ServerWebExchange exchange);

    @PostMapping(consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"})
    Mono<Void> register(@PathVariable("applicationId") String applicationId,
                        @RequestBody RegisterServiceIdentityDTO request, ServerWebExchange exchange);

    @DeleteMapping(value = "/{clientId}",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"})
    Mono<Void> remove(@PathVariable("applicationId") String applicationId,
                      @PathVariable("clientId") String clientId, ServerWebExchange exchange);
}
