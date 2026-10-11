package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.primaryadapters.ResolveMessageController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RestController;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;

@RestController
final class ResolveMessageControllerImpl implements ResolveMessageController {

    private final ResolveMessageUseCaseFacade facade;

    public ResolveMessageControllerImpl(ResolveMessageUseCaseFacade facade) {
        this.facade = facade;
    }

    @Override
    public ResolvedMessageDTO resolve(String code, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        var context = new MessageAccessContext(
                (String) request.getAttribute(ENVIRONMENT_ID_ATTRIBUTE),
                (ExternalIdentity) request.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE));
        return facade.execute(code, context);
    }
}
