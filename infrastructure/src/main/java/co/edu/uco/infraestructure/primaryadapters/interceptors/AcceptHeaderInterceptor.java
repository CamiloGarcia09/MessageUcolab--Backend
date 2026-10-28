package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.Optional;
import java.util.regex.Pattern;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_GET_HEADER_ACCEPT;

@Component
@Order(-100)
public final class AcceptHeaderInterceptor implements WebFilter {
    private static final Pattern WORKBOOK_DOWNLOAD_PATH = Pattern.compile(
            "/messageucolab/v1/messages/imports/(?:template|"
                    + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/errors)");
    private final SerializerRegistry serializers;
    private final CatalogPort catalog;
    private final LoggingPort log;
    public AcceptHeaderInterceptor(SerializerRegistry serializers, CatalogPort catalog, LoggingPortFactory factory) {
        this.serializers = serializers; this.catalog = catalog; this.log = factory.getLogger(getClass());
    }
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/messageucolab/v1/")) {
            return chain.filter(exchange);
        }
        String accept = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(REQUEST_GET_HEADER_ACCEPT))
                .orElse(MediaType.APPLICATION_JSON_VALUE);
        // Binary responses use WebFlux's own produces/content negotiation, not text serializers.
        // Only these GET mappings bypass this format filter; session and PEP filters still run.
        if (isServerSentEventsRequest(exchange, accept) || isWorkbookDownloadRequest(exchange)) {
            return chain.filter(exchange);
        }
        var serializer = serializers.getSerializerForMediaType(accept);
        if (serializer.supports(accept)) return chain.filter(exchange);
        exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType(serializer.getSupportedContentType()));
        log.error(catalog.getMessage(MessageCatalogCodeEnum.TCH_023.getCode()), accept);
        return exchange.getResponse().setComplete();
    }

    private boolean isServerSentEventsRequest(ServerWebExchange exchange, String accept) {
        return exchange.getRequest().getPath().value().endsWith("/messages/events")
                && MediaType.TEXT_EVENT_STREAM_VALUE.equalsIgnoreCase(accept);
    }

    private boolean isWorkbookDownloadRequest(ServerWebExchange exchange) {
        return HttpMethod.GET.equals(exchange.getRequest().getMethod())
                && WORKBOOK_DOWNLOAD_PATH.matcher(exchange.getRequest().getPath().value()).matches();
    }
}
