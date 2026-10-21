package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.AuthorizationScopeType;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingActiveContextPort;
import co.edu.uco.application.usecase.validator.authorization.AuthorizationCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class MessageEnvironmentResolverImpl implements MessageEnvironmentResolver {

    private final HandlingActiveContextPort activeContextPort;
    private final AuthorizationCompositeValidator authorizationRule;
    private final CatalogPort catalogPort;

    public MessageEnvironmentResolverImpl(final HandlingActiveContextPort activeContextPort,
                                           final AuthorizationCompositeValidator authorizationRule,
                                          final CatalogPort catalogPort) {
        this.activeContextPort = activeContextPort;
        this.authorizationRule = authorizationRule;
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
        throw UnauthorizedException.buildUserException(
                catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
    }
}
