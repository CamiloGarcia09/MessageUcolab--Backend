package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class SessionApplicationIdUuidRule extends RuleValidator<SessionApplicationValidationContext> {

    public SessionApplicationIdUuidRule(final CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(SessionApplicationValidationContext::applicationId, new ValidUuidSpecification()),
                MessageCatalogCodeEnum.FUN_038);
    }
}
