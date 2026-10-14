package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.token.FindEnvironmentIdTokenUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_152;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

final class EnvironmentContextResolver {

    private final FindEnvironmentIdTokenUseCaseFacade legacyEnvironment;
    private final ActiveContextUseCaseFacade activeContext;
    private final CatalogPort catalog;

    EnvironmentContextResolver(final FindEnvironmentIdTokenUseCaseFacade legacyEnvironment,
                                final ActiveContextUseCaseFacade activeContext,
                                final CatalogPort catalog) {
        this.legacyEnvironment = legacyEnvironment;
        this.activeContext = activeContext;
        this.catalog = catalog;
    }

    String resolveLegacy(final String token) {
        return requireEnvironment(legacyEnvironment.execute(token));
    }

    String resolveExternal(final ExternalIdentity identity) {
        ActiveContextDTO context = activeContext.findActiveContext(identity);
        return requireEnvironment(context == null ? null : context.getEnvironmentId());
    }

    private String requireEnvironment(final String environmentId) {
        if (isEmptyOrNull(environmentId)) {
            throw UnauthorizedException.buildUserException(catalog.getMessage(FUN_152.getCode()));
        }
        return environmentId;
    }
}
