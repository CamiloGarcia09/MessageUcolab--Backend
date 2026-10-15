package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;

public record UpdateMessageValidationContext(UpdateMessageDTO dto, String applicationId) {}
