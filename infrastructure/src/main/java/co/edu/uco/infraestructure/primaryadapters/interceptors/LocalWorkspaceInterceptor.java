package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.infraestructure.config.LocalWorkspaceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.LOCAL_WORKSPACE_PATH;

@Component
@Profile("local-workspace")
@Order(0)
public final class LocalWorkspaceInterceptor implements WebFilter {

    private static final String API_PREFIX = "/messageucolab/v1/";
    private final LocalWorkspaceProperties workspace;

    public LocalWorkspaceInterceptor(final LocalWorkspaceProperties workspace) {
        this.workspace = workspace;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }
        if (!isWorkspaceOperation(path)) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        // Do not resolve the old per-user active context in this explicit local mode.
        exchange.getAttributes().remove(EXTERNAL_IDENTITY_ATTRIBUTE);
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, workspace.environmentId().toString());
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, workspace.applicationId().toString());
        return chain.filter(exchange);
    }

    private boolean isWorkspaceOperation(final String path) {
        return LOCAL_WORKSPACE_PATH.equals(path)
                || (API_PREFIX + "messages").equals(path)
                || path.startsWith(API_PREFIX + "messages/")
                || path.startsWith(API_PREFIX + "catalog/")
                || (API_PREFIX + "application").equals(path)
                || (API_PREFIX + "application/functionality").equals(path);
    }
}
