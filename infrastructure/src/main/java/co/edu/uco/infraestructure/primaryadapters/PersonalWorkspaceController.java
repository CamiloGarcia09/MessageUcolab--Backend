package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("/messageucolab/v1")
public interface PersonalWorkspaceController {
    @GetMapping(value = "/session/user", produces = "application/json")
    Mono<Void> currentUser(ServerWebExchange exchange);

    @GetMapping(value = "/organizations", produces = "application/json")
    Mono<Void> organizations(ServerWebExchange exchange);

    @PostMapping(value = "/organizations", consumes = "application/json", produces = "application/json")
    Mono<Void> createOrganization(@RequestBody CreateOrganizationDTO dto, ServerWebExchange exchange);

    @GetMapping(value = "/organizations/{organizationId}/applications", produces = "application/json")
    Mono<Void> applications(@PathVariable("organizationId") String organizationId, ServerWebExchange exchange);
}
