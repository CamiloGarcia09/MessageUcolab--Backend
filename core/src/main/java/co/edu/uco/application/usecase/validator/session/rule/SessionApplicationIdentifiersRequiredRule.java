package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class SessionApplicationIdentifiersRequiredRule extends RuleValidator<SessionApplicationValidationContext> {

    public SessionApplicationIdentifiersRequiredRule(final CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null
                        && required(context.applicationId())
                        && required(context.environmentId()),
                MessageCatalogCodeEnum.FUN_155);
    }

    private static boolean required(final String value) {
        return new TextRequiredSpecification().isSatisfiedBy(value);
    }
}
