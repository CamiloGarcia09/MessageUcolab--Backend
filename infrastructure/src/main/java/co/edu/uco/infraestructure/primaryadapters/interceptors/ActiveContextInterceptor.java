package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@Component
@Order(0)
@Profile("!local-workspace")
public final class ActiveContextInterceptor implements WebFilter {

    private final EnvironmentContextResolver environment;

    public ActiveContextInterceptor(final ActiveContextUseCaseFacade activeContext, final CatalogPort catalog) {
        this.environment = new EnvironmentContextResolver(activeContext, catalog);
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/messageucolab/v1/")) {
            return chain.filter(exchange);
        }
        ExternalIdentity identity = exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE);
        if (identity == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        if (path.startsWith("/messageucolab/v1/me/")) {
            return chain.filter(exchange);
        }
        return Mono.fromCallable(() -> environment.resolveExternal(identity))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(id -> {
                    exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, id);
                    return chain.filter(exchange);
                })
                .onErrorResume(NotFoundException.class, error -> reject(exchange, HttpStatus.UNAUTHORIZED))
                .onErrorResume(ForbiddenException.class, error -> reject(exchange, HttpStatus.FORBIDDEN))
                .onErrorResume(UnauthorizedException.class, error -> reject(exchange, HttpStatus.UNAUTHORIZED));
    }

    private Mono<Void> reject(final ServerWebExchange exchange, final HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
