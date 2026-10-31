package co.edu.uco.application.usecase.validator.message.environment;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.message.environment.rule.MessageEnvironmentsBelongApplicationRule;
import co.edu.uco.application.usecase.validator.message.environment.rule.MessageEnvironmentsDuplicatedRule;
import co.edu.uco.application.usecase.validator.message.environment.rule.MessageEnvironmentsExistRule;
import co.edu.uco.application.usecase.validator.message.environment.rule.MessageEnvironmentsRequiredRule;
import co.edu.uco.application.usecase.validator.message.environment.rule.MessageEnvironmentsUuidRule;

import java.util.List;

/**
 * Ambientes donde se crea o se enlaza un mensaje. Lo comparten la creacion y la edicion;
 * el formato se valida antes de consultar el repositorio.
 */
public final class MessageEnvironmentSelectionCompositeValidator
        extends CompositeValidator<MessageEnvironmentSelectionContext> {

    public MessageEnvironmentSelectionCompositeValidator(final CatalogPort catalogPort,
                                                         final EnvironmentRepository environmentRepository) {
        super(List.of(
                new MessageEnvironmentsRequiredRule(catalogPort),
                new MessageEnvironmentsUuidRule(catalogPort),
                new MessageEnvironmentsDuplicatedRule(catalogPort),
                new MessageEnvironmentsExistRule(catalogPort, environmentRepository),
                new MessageEnvironmentsBelongApplicationRule(catalogPort, environmentRepository)
        ), catalogPort);
    }

    public void validate(final List<String> environmentIds, final String applicationId) {
        super.validate(new MessageEnvironmentSelectionContext(environmentIds, applicationId));
    }
}
