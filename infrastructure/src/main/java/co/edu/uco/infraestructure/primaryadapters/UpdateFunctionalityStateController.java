package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("${crosswords.api.path.functionality}")
public interface UpdateFunctionalityStateController {
    @PatchMapping(value = "/{functionalityId}/state", consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"})
    Mono<Void> updateState(@PathVariable("functionalityId") String functionalityId,
                          @RequestBody UpdateFunctionalityStateDTO dto, ServerWebExchange exchange);
}
