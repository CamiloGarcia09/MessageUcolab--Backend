package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_HEADER_APPLICATION_ID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_HEADER_ENVIRONMENT_ID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SECURITY_SESSION_COOKIE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;

/**
 * Modo sesion: el componente de seguridad informa las aplicaciones del usuario y el
 * cliente elige una de ellas. El servidor valida esa eleccion antes de fijar la aplicacion
 * de negocio; nunca acepta una aplicacion fuera de la sesion. El ambiente es un dato de
 * cada mensaje y solo se valida cuando la peticion lo envia explicitamente.
 */
@Component
@Profile("security-session")
@Order(0)
public final class SessionApplicationInterceptor implements WebFilter {

    private static final String API_PREFIX = "/messageucolab/v1/";
    private final SessionApplicationUseCaseFacade facade;

    public SessionApplicationInterceptor(final SessionApplicationUseCaseFacade facade) {
        this.facade = facade;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }
        exchange.getAttributes().remove(EXTERNAL_IDENTITY_ATTRIBUTE);
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(SECURITY_SESSION_COOKIE);
        String session = cookie == null ? null : cookie.getValue();
        var headers = exchange.getRequest().getHeaders();
        String applicationId = headers.getFirst(REQUEST_HEADER_APPLICATION_ID);
        String environmentId = headers.getFirst(REQUEST_HEADER_ENVIRONMENT_ID);
        boolean requiresApplication = isMessageOperation(path);

        // Solo los errores de esta resolucion se traducen aqui; los del controlador siguen su flujo.
        return Mono.fromCallable(() -> {
                    var applications = facade.findCurrentUserApplications(session);
                    exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, applications);
                    if (requiresApplication) {
                        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE,
                                facade.resolveApplication(applications, applicationId));
                        if (environmentId != null && !environmentId.isBlank()) {
                            exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE,
                                    facade.resolveEnvironment(applications, applicationId, environmentId));
                        }
                    }
                    return Optional.<HttpStatus>empty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(CrossWordsException.class, error -> Mono.just(Optional.of(statusOf(error))))
                .flatMap(rejection -> rejection.isPresent()
                        ? reject(exchange, rejection.get())
                        : chain.filter(exchange));
    }

    private HttpStatus statusOf(final CrossWordsException error) {
        if (error instanceof UnauthorizedException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (error instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (error instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (error instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (error instanceof BusinessRuleException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    private boolean isMessageOperation(final String path) {
        return (API_PREFIX + "messages").equals(path) || path.startsWith(API_PREFIX + "messages/");
    }

    private Mono<Void> reject(final ServerWebExchange exchange, final HttpStatus status) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
