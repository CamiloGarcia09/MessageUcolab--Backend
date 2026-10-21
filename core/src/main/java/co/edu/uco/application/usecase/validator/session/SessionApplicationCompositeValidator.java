package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationAssignedRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationExistsRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdUuidRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdentifiersRequiredRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentBelongsApplicationRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentExistsRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentIdUuidRule;

import java.util.List;

/**
 * Valida la aplicacion y el ambiente que el cliente selecciona dentro de la sesion.
 * La pertenencia a la sesion se comprueba antes de consultar repositorios.
 */
public final class SessionApplicationCompositeValidator extends CompositeValidator<SessionApplicationValidationContext> {

    public SessionApplicationCompositeValidator(final CatalogPort catalogPort,
                                                final ApplicationRepository applicationRepository,
                                                final EnvironmentRepository environmentRepository) {
        super(List.of(
                new SessionApplicationIdentifiersRequiredRule(catalogPort),
                new SessionApplicationIdUuidRule(catalogPort),
                new SessionEnvironmentIdUuidRule(catalogPort),
                new SessionApplicationAssignedRule(catalogPort),
                new SessionApplicationExistsRule(catalogPort, applicationRepository),
                new SessionEnvironmentExistsRule(catalogPort, environmentRepository),
                new SessionEnvironmentBelongsApplicationRule(catalogPort, environmentRepository)
        ), catalogPort);
    }
}
