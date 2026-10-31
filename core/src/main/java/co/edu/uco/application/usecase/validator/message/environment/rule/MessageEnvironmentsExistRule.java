package co.edu.uco.application.usecase.validator.message.environment.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

public final class MessageEnvironmentsExistRule extends RuleValidator<MessageEnvironmentSelectionContext> {

    public MessageEnvironmentsExistRule(final CatalogPort catalogPort,
                                        final EnvironmentRepository environmentRepository) {
        super(catalogPort,
                context -> context != null && context.environmentIds() != null
                        && context.environmentIds().stream()
                        .allMatch(environmentId -> environmentRepository.findById(environmentId).isPresent()),
                MessageCatalogCodeEnum.FUN_035,
                NotFoundException::buildUserException);
    }
}
