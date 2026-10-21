package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationAssignedRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdRequiredRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdUuidRule;

import java.util.List;

/**
 * Solo permite registrar en el catalogo una aplicacion asignada al usuario de la sesion,
 * usando el mismo UUID que tiene en el componente de seguridad.
 */
public final class SessionApplicationRegistrationCompositeValidator
        extends CompositeValidator<SessionApplicationValidationContext> {

    public SessionApplicationRegistrationCompositeValidator(final CatalogPort catalogPort) {
        super(List.of(
                new SessionApplicationIdRequiredRule(catalogPort),
                new SessionApplicationIdUuidRule(catalogPort),
                new SessionApplicationAssignedRule(catalogPort)
        ), catalogPort);
    }
}
