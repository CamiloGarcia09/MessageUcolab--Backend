package co.edu.uco.application.usecase.validator.consumer.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ConsumerEnvironmentIdRequiredRule extends RuleValidator<SessionApplicationValidationContext> {

    public ConsumerEnvironmentIdRequiredRule(final CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(SessionApplicationValidationContext::environmentId,
                        new TextRequiredSpecification()),
                MessageCatalogCodeEnum.FUN_187);
    }
}
