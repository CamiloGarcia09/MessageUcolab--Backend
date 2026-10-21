package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.secondaryports.security.ExternalIdentityResolverPort;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.regex.Pattern;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@Component
@Order(-200)
@Profile("!local-workspace & !security-session")
@ConditionalOnProperty(prefix = "components.security.simulated", name = "enabled", havingValue = "true")
public final class ExternalIdentityInterceptor implements WebFilter {
    private static final Pattern BEARER = Pattern.compile("^Bearer (.+)$", Pattern.CASE_INSENSITIVE);
    private final ExternalIdentityResolverPort resolver;
    public ExternalIdentityInterceptor(ExternalIdentityResolverPort resolver) { this.resolver = resolver; }
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String value = exchange.getRequest().getHeaders().getFirst(REQUEST_GET_HEADER_AUTHORIZATION);
        if (value == null || value.isBlank()) return chain.filter(exchange);
        var matcher = BEARER.matcher(value);
        if (!matcher.matches()) return unauthorized(exchange);
        return Mono.fromCallable(() -> resolver.resolve(matcher.group(1)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(identity -> { exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, identity); return chain.filter(exchange); })
                .onErrorResume(UnauthorizedException.class, error -> unauthorized(exchange));
    }
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
