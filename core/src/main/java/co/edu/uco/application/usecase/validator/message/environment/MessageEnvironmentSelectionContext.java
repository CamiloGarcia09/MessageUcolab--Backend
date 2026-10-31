package co.edu.uco.application.usecase.validator.message.environment;

import java.util.List;

public record MessageEnvironmentSelectionContext(List<String> environmentIds, String applicationId) {
}
