package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingFindMessageByCodeAndApplicationPort {
    MessageDTO execute(String messageCode, MessageAccessContext context);
}
