package co.edu.uco.application.usecase.validator.workspace;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;

public final class SessionIdentityCompositeValidator extends CompositeValidator<SessionIdentity> {
    public SessionIdentityCompositeValidator(CatalogPort catalogPort) {
        super(List.of(new IdentityRequiredRule(catalogPort)), catalogPort);
    }

    private static final class IdentityRequiredRule extends RuleValidator<SessionIdentity> {
        private IdentityRequiredRule(CatalogPort catalogPort) {
            super(catalogPort, identity -> identity != null
                            && new TextRequiredSpecification().isSatisfiedBy(identity.source())
                            && new TextRequiredSpecification().isSatisfiedBy(identity.tenantId())
                            && new TextRequiredSpecification().isSatisfiedBy(identity.subject()),
                    MessageCatalogCodeEnum.FUN_152, UnauthorizedException::buildUserException);
        }
    }
}
