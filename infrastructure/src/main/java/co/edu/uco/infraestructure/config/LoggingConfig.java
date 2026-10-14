package co.edu.uco.infraestructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.UUID;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@Component
@Order(-300)
public final class LoggingConfig implements WebFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingConfig.class);
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlation = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlation == null || correlation.isBlank()) correlation = UUID.randomUUID().toString();
        exchange.getResponse().getHeaders().set(CORRELATION_ID, correlation);
        exchange.getResponse().getHeaders().set(LOGGING_APP_NAME, LOGGING_PARAMETER_APPLICATION_NAME);
        final String id = correlation;
        return chain.filter(exchange)
                .doOnEach(signal -> {
                    if (signal.isOnComplete() || signal.isOnError()) {
                        try (MDC.MDCCloseable ignored = MDC.putCloseable(CORRELATION_ID, id)) {
                            LOG.info("Request completed with status {}", exchange.getResponse().getStatusCode());
                        }
                    }
                });
    }
}
