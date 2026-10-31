package co.edu.uco.application.secondaryports;

import java.util.List;

public record Response<T>(List<T> data, List<String> errors) {

    public Response(List<T>  data) {
        this(data, List.of());
    }
}
