package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.secondaryports.repository.SimplePageRequest;

public record SimplePageRequestValidationContext(SimplePageRequest request, Class<?> modelClass) {
}
