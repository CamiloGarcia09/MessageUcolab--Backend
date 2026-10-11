package co.edu.uco.infraestructure.secondaryadapters.presenter.rest;

import co.edu.uco.application.secondaryports.ErrorResponse;
import co.edu.uco.application.secondaryports.Response;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_GET_HEADER_ACCEPT;

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
    public void presentRestSuccess(
            List<T> dto,
            HttpServletRequest  request,
            HttpServletResponse response
    ) {
        try {
            var acceptHeader = request.getHeader(REQUEST_GET_HEADER_ACCEPT);
            var serializer = serializerRegistry.getSerializerForMediaType(acceptHeader);

            var responseBody = new Response<>(dto, Collections.emptyList());
            var formattedResponse = serializer.serialize(responseBody);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(serializer.getSupportedContentType());
            response.getWriter().write(formattedResponse);
            log.info(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_021.getCode()));

        } catch (CrossWordsException | IOException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), ex);
        }
    }

    @ExceptionHandler(CrossWordsException.class)
    public void presentCrossWordsException(
            CrossWordsException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            var acceptHeader = request.getHeader(REQUEST_GET_HEADER_ACCEPT);
            var serializer = serializerRegistry.getSerializerForMediaType(acceptHeader);

            var status = ex.getHttpStatus() == 0 ? HttpStatus.INTERNAL_SERVER_ERROR.value() : ex.getHttpStatus();
            var userMessage = Optional.ofNullable(ex.getUserMessage())
                    .filter(msg -> !msg.isEmpty())
                    .orElseGet(() -> catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()));
            var code = Optional.ofNullable(ex.getCode())
                    .filter(c -> !c.isEmpty())
                    .orElseGet(() -> ErrorResponseFactory.codeForStatus(status));

            var errorResponse = ErrorResponseFactory.build(code, userMessage, request);
            var formattedResponse = serializer.serialize(errorResponse);
            response.setStatus(status);
            response.setContentType(serializer.getSupportedContentType());
            response.getWriter().write(formattedResponse);

            if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), ex);
            }
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_020.getCode()));
        } catch (IOException | CrossWordsException exception) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_019.getCode()), exception);
            throw exception;
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        writeClientError(HttpStatus.BAD_REQUEST.value(), request, response, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        writeClientError(HttpStatus.BAD_REQUEST.value(), request, response, ex);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        writeClientError(HttpStatus.NOT_ACCEPTABLE.value(), request, response, ex);
    }

    @ExceptionHandler(Exception.class)
    public void handleGeneralException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            var acceptHeader = request.getHeader(REQUEST_GET_HEADER_ACCEPT);
            var serializer = serializerRegistry.getSerializerForMediaType(acceptHeader);

            var message = catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode());
            var errorResponse = ErrorResponseFactory.build(
                    ErrorResponseFactory.codeForStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                    message,
                    request
            );
            var formattedResponse = serializer.serialize(errorResponse);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(serializer.getSupportedContentType());
            response.getWriter().write(formattedResponse);
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), ex);
        } catch (IOException ioEx) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_019.getCode()), ioEx);
        }
    }

    private void writeClientError(int status, HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
        try {
            var acceptHeader = request.getHeader(REQUEST_GET_HEADER_ACCEPT);
            var serializer = serializerRegistry.getSerializerForMediaType(acceptHeader);

            var userMessage = catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode());
            var errorResponse = ErrorResponseFactory.build(
                    ErrorResponseFactory.codeForStatus(status),
                    userMessage,
                    request
            );
            var formattedResponse = serializer.serialize(errorResponse);
            response.setStatus(status);
            response.setContentType(serializer.getSupportedContentType());
            response.getWriter().write(formattedResponse);
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_016.getCode()), ex);
        } catch (IOException | CrossWordsException exception) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_019.getCode()), exception);
            throw exception;
        }
    }
}
