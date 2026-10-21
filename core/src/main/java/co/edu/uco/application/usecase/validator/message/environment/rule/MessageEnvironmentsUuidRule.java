package co.edu.uco.application.usecase.validator.message.environment.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class MessageEnvironmentsUuidRule extends RuleValidator<MessageEnvironmentSelectionContext> {

    public MessageEnvironmentsUuidRule(final CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.environmentIds() != null
                        && context.environmentIds().stream().allMatch(new ValidUuidSpecification()::isSatisfiedBy),
                MessageCatalogCodeEnum.FUN_038);
    }
}
