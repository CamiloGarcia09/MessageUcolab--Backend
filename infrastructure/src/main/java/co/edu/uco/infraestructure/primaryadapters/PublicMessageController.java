package co.edu.uco.infraestructure.primaryadapters;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_HEADER_APPLICATION_ID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_HEADER_ENVIRONMENT_ID;

/**
 * Consulta de mensajes para aplicaciones consumidoras, sin sesion BFF.
 * Los encabezados se validan en el caso de uso para responder con el mensaje del catalogo.
 */
@RequestMapping("${crosswords.api.path.public-messages}")
public interface PublicMessageController {

    @GetMapping(produces = "application/json")
    Mono<Void> list(@RequestHeader(value = REQUEST_HEADER_APPLICATION_ID, required = false) String applicationId,
                    @RequestHeader(value = REQUEST_HEADER_ENVIRONMENT_ID, required = false) String environmentId,
                    ServerWebExchange exchange);

    @GetMapping(value = "/{code}", produces = "application/json")
    Mono<Void> find(@PathVariable("code") String code,
                    @RequestParam(value = "language", required = false) String language,
                    @RequestHeader(value = REQUEST_HEADER_APPLICATION_ID, required = false) String applicationId,
                    @RequestHeader(value = REQUEST_HEADER_ENVIRONMENT_ID, required = false) String environmentId,
                    ServerWebExchange exchange);
}
