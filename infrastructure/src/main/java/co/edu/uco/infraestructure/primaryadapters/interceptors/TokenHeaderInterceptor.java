package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.facade.token.*;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
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
    private final FindEnvironmentIdTokenUseCaseFacade environment;
    private final CatalogPort catalog;
    public TokenHeaderInterceptor(VerifyAccessUseCaseFacade verify, FindEnvironmentIdTokenUseCaseFacade environment,
                                  CatalogPort catalog) {
        this.verify = verify; this.environment = environment; this.catalog = catalog;
    }
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/messageucolab/v1/")) {
            return chain.filter(exchange);
        }
        if (exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE) != null) return chain.filter(exchange);
        String token = exchange.getRequest().getHeaders().getFirst(REQUEST_GET_HEADER_TOKEN);
        if (isEmptyOrNull(token)) return unauthorized(exchange);
        return Mono.fromCallable(() -> {
            if (!verify.execute(token)) throw UnauthorizedException.buildUserException(
                    catalog.getMessage(MessageCatalogCodeEnum.TCH_031.getCode()));
            return environment.execute(token);
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(id -> {
            exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, id);
            return chain.filter(exchange);
        }).onErrorResume(NotFoundException.class, error -> unauthorized(exchange))
          .onErrorResume(UnauthorizedException.class, error -> unauthorized(exchange));
    }
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
