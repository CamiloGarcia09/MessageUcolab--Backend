package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class ReactiveHttpResponse {

    private ReactiveHttpResponse() {
    }

    public static <T> Mono<Void> present(PresenterPort<T> presenter, List<T> body,
                                         ServerWebExchange exchange) {
        String accept = exchange.getRequest().getHeaders().getFirst("Accept");
        return Mono.fromCallable(() -> presenter.presentRestSuccess(body, accept))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(rendered -> write(exchange, rendered.body(), rendered.contentType()));
    }

    public static <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }

    public static Object attribute(ServerWebExchange exchange, String name) {
        return exchange.getAttributes().get(name);
    }

    private static Mono<Void> write(ServerWebExchange exchange, String body, String contentType) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType(contentType));
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
