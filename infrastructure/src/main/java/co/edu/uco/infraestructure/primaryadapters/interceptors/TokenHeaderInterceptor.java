package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.token.FindEnvironmentIdTokenUseCaseFacade;
import co.edu.uco.application.primaryports.facade.token.VerifyAccessUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Optional;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

@Component
@Order(0)
public final class TokenHeaderInterceptor implements WebFilter {
    private final VerifyAccessUseCaseFacade verify;
    private final EnvironmentContextResolver environment;
    private final CatalogPort catalog;
    public TokenHeaderInterceptor(VerifyAccessUseCaseFacade verify,
                                  FindEnvironmentIdTokenUseCaseFacade legacyEnvironment,
                                  ActiveContextUseCaseFacade activeContext,
                                  CatalogPort catalog) {
        this.verify = verify;
        this.environment = new EnvironmentContextResolver(legacyEnvironment, activeContext, catalog);
        this.catalog = catalog;
    }
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/messageucolab/v1/")) {
            return chain.filter(exchange);
        }
        var externalIdentity = exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE);
        if (externalIdentity != null) {
            if (isContextEndpoint(exchange)) {
                return chain.filter(exchange);
            }
            return Mono.fromCallable(() -> environment.resolveExternal(
                            (co.edu.uco.application.usecase.domain.security.ExternalIdentity) externalIdentity))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(id -> continueWithEnvironment(exchange, chain, id))
                    .onErrorResume(NotFoundException.class, error -> unauthorized(exchange))
                    .onErrorResume(ForbiddenException.class, error -> forbidden(exchange))
                    .onErrorResume(UnauthorizedException.class, error -> unauthorized(exchange));
        }
        String token = exchange.getRequest().getHeaders().getFirst(REQUEST_GET_HEADER_TOKEN);
        if (isEmptyOrNull(token)) return unauthorized(exchange);
        return Mono.fromCallable(() -> {
            if (!verify.execute(token)) throw UnauthorizedException.buildUserException(
                    catalog.getMessage(MessageCatalogCodeEnum.TCH_031.getCode()));
            if (isContextEndpoint(exchange)) return null;
            return environment.resolveLegacy(token);
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(id -> {
            if (id == null) return chain.filter(exchange);
            return continueWithEnvironment(exchange, chain, id);
        }).onErrorResume(NotFoundException.class, error -> unauthorized(exchange))
          .onErrorResume(ForbiddenException.class, error -> forbidden(exchange))
          .onErrorResume(UnauthorizedException.class, error -> unauthorized(exchange));
    }
    private Mono<Void> continueWithEnvironment(ServerWebExchange exchange, WebFilterChain chain, String id) {
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, id);
        return chain.filter(exchange);
    }
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean isContextEndpoint(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith("/messageucolab/v1/me/");
    }
}
