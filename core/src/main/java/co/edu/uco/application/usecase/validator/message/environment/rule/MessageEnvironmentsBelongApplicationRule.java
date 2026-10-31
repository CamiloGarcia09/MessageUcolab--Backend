package co.edu.uco.application.usecase.validator.message.environment.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * Impide crear o enlazar mensajes en ambientes de otra aplicacion.
 */
public final class MessageEnvironmentsBelongApplicationRule extends RuleValidator<MessageEnvironmentSelectionContext> {

    public MessageEnvironmentsBelongApplicationRule(final CatalogPort catalogPort,
                                                    final EnvironmentRepository environmentRepository) {
        super(catalogPort,
                context -> belongToApplication(context, environmentRepository),
                MessageCatalogCodeEnum.FUN_036,
                ForbiddenException::buildUserException);
    }

    private static boolean belongToApplication(final MessageEnvironmentSelectionContext context,
                                               final EnvironmentRepository environmentRepository) {
        if (context == null || context.environmentIds() == null
                || !new ValidUuidSpecification().isSatisfiedBy(context.applicationId())) {
            return false;
        }
        var applicationId = getUUIDFromString(context.applicationId());
        return context.environmentIds().stream()
                .allMatch(environmentId -> environmentRepository.findById(environmentId)
                        .map(environment -> applicationId.equals(environment.getApplication().getId()))
                        .orElse(false));
    }
}
