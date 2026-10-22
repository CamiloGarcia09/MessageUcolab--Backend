package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class MessageEnvironmentResolverImpl implements MessageEnvironmentResolver {

    private final MessageEnvironmentSelectionCompositeValidator environmentValidator;
    private final CatalogPort catalogPort;

    public MessageEnvironmentResolverImpl(final MessageEnvironmentSelectionCompositeValidator environmentValidator,
                                          final CatalogPort catalogPort) {
        this.environmentValidator = environmentValidator;
        this.catalogPort = catalogPort;
    }

    @Override
    public String resolve(final MessageAccessContext context, final String requestedEnvironmentId) {
        if (context != null && !isEmptyOrNull(context.applicationId())
                && !isEmptyOrNull(requestedEnvironmentId)) {
            var applicationId = resolveApplication(context);
            environmentValidator.validate(List.of(requestedEnvironmentId), applicationId);
            return getUUIDFromString(requestedEnvironmentId).toString();
        }
        throw unauthorized();
    }

    @Override
    public String resolveApplication(final MessageAccessContext context) {
        // Modo sesion: el interceptor ya valido la aplicacion contra el componente de seguridad.
        if (context != null && !isEmptyOrNull(context.applicationId())) {
            return getUUIDFromString(context.applicationId()).toString();
        }
        throw unauthorized();
    }

    private UnauthorizedException unauthorized() {
        return UnauthorizedException.buildUserException(
                catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
    }
}
