package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface HandlingFindMessagesByApplicationPort {
    SimplePage<MessageDTO> execute(MessageAccessContext context, SimplePageRequest pageRequest);
}
