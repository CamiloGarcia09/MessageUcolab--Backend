package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationAssignedRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationExistsRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdRequiredRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdUuidRule;

import java.util.List;

/**
 * Valida la aplicacion seleccionada en la sesion, que es todo el contexto de los mensajes.
 * La pertenencia a la sesion se comprueba antes de consultar el repositorio.
 */
public final class SessionApplicationAccessCompositeValidator
        extends CompositeValidator<SessionApplicationValidationContext> {

    public SessionApplicationAccessCompositeValidator(final CatalogPort catalogPort,
                                                      final ApplicationRepository applicationRepository) {
        super(List.of(
                new SessionApplicationIdRequiredRule(catalogPort),
                new SessionApplicationIdUuidRule(catalogPort),
                new SessionApplicationAssignedRule(catalogPort),
                new SessionApplicationExistsRule(catalogPort, applicationRepository)
        ), catalogPort);
    }
}
