package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.token.CreateTokenDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.FIELD_ID;

@RequestMapping("${crosswords.api.path.application}")
public interface TokenController {

    @PostMapping(
            value = "${crosswords.api.path.token.application}",
            consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> createToken(
            @RequestBody CreateTokenDTO tokenDTO,
            @PathVariable(FIELD_ID) String applicationId,
            ServerWebExchange exchange
    );
}
