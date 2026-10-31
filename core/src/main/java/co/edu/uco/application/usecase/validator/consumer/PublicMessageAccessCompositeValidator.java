package co.edu.uco.application.usecase.validator.consumer;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.consumer.rule.ConsumerEnvironmentIdRequiredRule;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationExistsRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdRequiredRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionApplicationIdUuidRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentBelongsApplicationRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentExistsRule;
import co.edu.uco.application.usecase.validator.session.rule.SessionEnvironmentIdUuidRule;

import java.util.List;

/**
 * Aplicacion y ambiente que envia una aplicacion consumidora sin sesion BFF.
 * Reutiliza las reglas de la sesion salvo la propiedad: no hay un usuario del que comprobarla.
 */
public final class PublicMessageAccessCompositeValidator extends CompositeValidator<SessionApplicationValidationContext> {

    public PublicMessageAccessCompositeValidator(final CatalogPort catalogPort,
                                                 final ApplicationRepository applicationRepository,
                                                 final EnvironmentRepository environmentRepository) {
        super(List.of(
                new SessionApplicationIdRequiredRule(catalogPort),
                new ConsumerEnvironmentIdRequiredRule(catalogPort),
                new SessionApplicationIdUuidRule(catalogPort),
                new SessionEnvironmentIdUuidRule(catalogPort),
                new SessionApplicationExistsRule(catalogPort, applicationRepository),
                new SessionEnvironmentExistsRule(catalogPort, environmentRepository),
                new SessionEnvironmentBelongsApplicationRule(catalogPort, environmentRepository)
        ), catalogPort);
    }

    public void validate(final String applicationId, final String environmentId) {
        super.validate(new SessionApplicationValidationContext(List.of(), applicationId, environmentId));
    }
}
