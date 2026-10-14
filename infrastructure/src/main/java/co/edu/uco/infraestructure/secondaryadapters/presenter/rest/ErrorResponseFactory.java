package co.edu.uco.infraestructure.secondaryadapters.presenter.rest;

import co.edu.uco.application.secondaryports.ErrorDetail;
import co.edu.uco.application.secondaryports.ErrorResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.PATTERN_TIMESTAMP_FORMAT;

public final class ErrorResponseFactory {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern(PATTERN_TIMESTAMP_FORMAT)
            .withZone(ZoneOffset.UTC);

    private ErrorResponseFactory() {}

    public static ErrorResponse build(String code, String message, ServerHttpRequest request) {
        var path = request != null && request.getPath() != null ? request.getPath().value() : "";
        return ErrorResponse.of(List.of(new ErrorDetail(code, message)), TIMESTAMP_FORMAT.format(Instant.now()), path);
    }

    public static String codeForStatus(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 406 -> "NOT_ACCEPTABLE";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            default -> "INTERNAL_SERVER_ERROR";
        };
    }
}