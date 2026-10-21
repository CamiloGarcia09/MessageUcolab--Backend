package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * La aplicacion solicitada debe estar entre las asignadas al usuario por el
 * componente de seguridad; la aplicacion de negocio comparte su UUID.
 */
public final class SessionApplicationAssignedRule extends RuleValidator<SessionApplicationValidationContext> {

    public SessionApplicationAssignedRule(final CatalogPort catalogPort) {
        super(catalogPort,
                SessionApplicationAssignedRule::isAssigned,
                MessageCatalogCodeEnum.FUN_153,
                ForbiddenException::buildUserException);
    }

    private static boolean isAssigned(final SessionApplicationValidationContext context) {
        if (context == null || context.sessionApplications() == null
                || !new ValidUuidSpecification().isSatisfiedBy(context.applicationId())) {
            return false;
        }
        var applicationId = getUUIDFromString(context.applicationId());
        return context.sessionApplications().stream()
                .map(CatalogItemDTO::getId)
                .filter(id -> new ValidUuidSpecification().isSatisfiedBy(id))
                .anyMatch(id -> applicationId.equals(getUUIDFromString(id)));
    }
}
