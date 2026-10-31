package co.edu.uco.application.secondaryports;

import java.util.List;

public record ErrorResponse(List<?> data, List<ErrorDetail> errors, String timestamp, String path) {

    public ErrorResponse {
        data = data == null ? List.of() : data;
        errors = errors == null ? List.of() : errors;
    }

    public static ErrorResponse of(List<ErrorDetail> errors, String timestamp, String path) {
        return new ErrorResponse(List.of(), errors, timestamp, path);
    }
}