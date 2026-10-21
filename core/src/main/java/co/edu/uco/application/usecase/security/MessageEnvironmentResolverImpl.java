package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.domain.security.AuthorizationScopeType;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingActiveContextPort;
import co.edu.uco.application.usecase.validator.authorization.AuthorizationCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class MessageEnvironmentResolverImpl implements MessageEnvironmentResolver {

    private final HandlingActiveContextPort activeContextPort;
    private final AuthorizationCompositeValidator authorizationRule;
    private final EnvironmentRepository environmentRepository;
    private final CatalogPort catalogPort;

    public MessageEnvironmentResolverImpl(final HandlingActiveContextPort activeContextPort,
                                           final AuthorizationCompositeValidator authorizationRule,
                                          final EnvironmentRepository environmentRepository,
                                          final CatalogPort catalogPort) {
        this.activeContextPort = activeContextPort;
        this.authorizationRule = authorizationRule;
        this.environmentRepository = environmentRepository;
        this.catalogPort = catalogPort;
    }

    @Override
    public String resolve(final MessageAccessContext context, final PermissionCode permission) {
        if (context != null && context.externalIdentity() != null) {
            var activeContext = activeContextPort.findActiveContext(context.externalIdentity());
            var environmentId = getUUIDFromString(activeContext.getEnvironmentId());
            authorizationRule.validate(context.externalIdentity(), permission,
                    AuthorizationScopeType.ENVIRONMENT, environmentId);
            return environmentId.toString();
        }
        if (context != null && !isEmptyOrNull(context.workspaceEnvironmentId())) {
            return context.workspaceEnvironmentId();
        }
        throw unauthorized();
    }

    @Override
    public String resolveApplication(final MessageAccessContext context, final PermissionCode permission) {
        // Modo sesion: el interceptor ya valido la aplicacion contra el componente de seguridad.
        if (context != null && !isEmptyOrNull(context.applicationId())) {
            return getUUIDFromString(context.applicationId()).toString();
        }
        if (context != null && context.externalIdentity() != null) {
            var activeContext = activeContextPort.findActiveContext(context.externalIdentity());
            var applicationId = getUUIDFromString(activeContext.getApplicationId());
            authorizationRule.validate(context.externalIdentity(), permission,
                    AuthorizationScopeType.APPLICATION, applicationId);
            return applicationId.toString();
        }
        if (context != null && !isEmptyOrNull(context.workspaceEnvironmentId())) {
            return environmentRepository.findById(context.workspaceEnvironmentId())
                    .map(environment -> environment.getApplication().getId().toString())
                    .orElseThrow(() -> ForbiddenException.buildUserException(
                            catalogPort.getMessage(MessageCatalogCodeEnum.FUN_035.getCode())));
        }
        throw unauthorized();
    }

    private UnauthorizedException unauthorized() {
        return UnauthorizedException.buildUserException(
                catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
    }
}
