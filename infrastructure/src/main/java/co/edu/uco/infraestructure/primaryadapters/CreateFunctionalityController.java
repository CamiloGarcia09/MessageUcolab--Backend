package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("${crosswords.api.path.functionality}")
public interface CreateFunctionalityController {

    @PostMapping(
            consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> createFunctionality(
            @RequestBody CreateFunctionalityDTO createFunctionalityDTO,
            ServerWebExchange exchange
    );
}