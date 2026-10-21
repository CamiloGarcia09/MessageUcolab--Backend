package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

public final class SessionEnvironmentExistsRule extends RuleValidator<SessionApplicationValidationContext> {

    public SessionEnvironmentExistsRule(final CatalogPort catalogPort,
                                        final EnvironmentRepository environmentRepository) {
        super(catalogPort,
                Specifications.field(SessionApplicationValidationContext::environmentId,
                        environmentId -> new ValidUuidSpecification().isSatisfiedBy(environmentId)
                                && environmentRepository.findById(environmentId).isPresent()),
                MessageCatalogCodeEnum.FUN_158,
                NotFoundException::buildUserException);
    }
}
