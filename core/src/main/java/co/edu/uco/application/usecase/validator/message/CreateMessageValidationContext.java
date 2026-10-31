package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;

public record CreateMessageValidationContext(CreateMessageDTO dto, String applicationId) {
}
