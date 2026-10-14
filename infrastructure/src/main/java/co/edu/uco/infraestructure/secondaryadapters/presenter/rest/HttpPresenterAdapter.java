package co.edu.uco.infraestructure.secondaryadapters.presenter.rest;

import co.edu.uco.application.secondaryports.ErrorResponse;
import co.edu.uco.application.secondaryports.Response;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestControllerAdvice
public final class HttpPresenterAdapter<T> implements PresenterPort<T> {

    private final LoggingPort log;
    private final SerializerRegistry serializerRegistry;
    private final CatalogPort catalogPort;

    public HttpPresenterAdapter(SerializerRegistry serializerRegistry, CatalogPort catalogPort,
                                LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(HttpPresenterAdapter.class);
        this.serializerRegistry = serializerRegistry;
        this.catalogPort = catalogPort;
    }

    @Override
    public RenderedResponse presentRestSuccess(List<T> dto, String acceptHeader) {
        try {
            var serializer = serializerRegistry.getSerializerForMediaType(acceptHeader);
            var responseBody = new Response<>(dto, Collections.emptyList());
            return new RenderedResponse(serializer.serialize(responseBody), serializer.getSupportedContentType());
        } catch (CrossWordsException exception) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), exception);
            throw exception;
        }
    }

    @ExceptionHandler(CrossWordsException.class)
    public Mono<ResponseEntity<String>> presentCrossWordsException(CrossWordsException ex,
                                                                    ServerWebExchange exchange) {
        int status = ex.getHttpStatus() == 0 ? 500 : ex.getHttpStatus();
        String message = Optional.ofNullable(ex.getUserMessage()).filter(value -> !value.isEmpty())
                .orElseGet(() -> catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()));
        return error(status, message, exchange, ex);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleGeneralException(Exception ex, ServerWebExchange exchange) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()), exchange, ex);
    }

    private Mono<ResponseEntity<String>> error(int status, String message,
                                               ServerWebExchange exchange, Exception exception) {
        try {
            String accept = exchange.getRequest().getHeaders().getFirst("Accept");
            var serializer = serializerRegistry.getSerializerForMediaType(accept);
            ErrorResponse response = ErrorResponseFactory.build(
                    ErrorResponseFactory.codeForStatus(status), message, exchange.getRequest());
            String body = serializer.serialize(response);
            if (status >= 500) {
                log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), exception);
            }
            return Mono.just(ResponseEntity.status(status)
                    .contentType(MediaType.parseMediaType(serializer.getSupportedContentType()))
                    .body(body));
        } catch (CrossWordsException exceptionDuringRendering) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_019.getCode()),
                    exceptionDuringRendering);
            return Mono.error(exceptionDuringRendering);
        }
    }
}
