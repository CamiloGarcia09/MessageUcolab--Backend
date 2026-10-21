package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class SessionEnvironmentBelongsApplicationRule
        extends RuleValidator<SessionApplicationValidationContext> {

    public SessionEnvironmentBelongsApplicationRule(final CatalogPort catalogPort,
                                                    final EnvironmentRepository environmentRepository) {
        super(catalogPort,
                context -> belongsToApplication(context, environmentRepository),
                MessageCatalogCodeEnum.FUN_160,
                ConflictException::buildUserException);
    }

    private static boolean belongsToApplication(final SessionApplicationValidationContext context,
                                                final EnvironmentRepository environmentRepository) {
        if (context == null
                || !new ValidUuidSpecification().isSatisfiedBy(context.applicationId())
                || !new ValidUuidSpecification().isSatisfiedBy(context.environmentId())) {
            return false;
        }
        return environmentRepository.findById(context.environmentId())
                .map(environment -> getUUIDFromString(context.applicationId())
                        .equals(environment.getApplication().getId()))
                .orElse(false);
    }
}
