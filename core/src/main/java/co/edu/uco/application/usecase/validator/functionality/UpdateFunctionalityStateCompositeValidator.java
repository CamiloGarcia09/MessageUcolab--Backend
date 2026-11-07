package co.edu.uco.application.usecase.validator.functionality;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

import java.util.List;
import java.util.function.Function;

import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isActive;
import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isInactive;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_038;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_185;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_226;

public final class UpdateFunctionalityStateCompositeValidator extends CompositeValidator<FunctionalityStateChange> {
    public UpdateFunctionalityStateCompositeValidator(CatalogPort catalog,
                                                       UpdateFunctionalityStateRepository repository) {
        super(List.of(new IdRule(catalog, FunctionalityStateChange::applicationId),
                new IdRule(catalog, FunctionalityStateChange::functionalityId),
                new IdRule(catalog, FunctionalityStateChange::stateId),
                new FunctionalityRule(catalog, repository), new StateRule(catalog, repository)), catalog);
    }

    private static final class IdRule extends RuleValidator<FunctionalityStateChange> {
        private IdRule(CatalogPort catalog, Function<FunctionalityStateChange, String> extractor) {
            super(catalog, change -> new ValidUuidSpecification().isSatisfiedBy(extractor.apply(change)), FUN_038);
        }
    }

    private static final class FunctionalityRule extends RuleValidator<FunctionalityStateChange> {
        private FunctionalityRule(CatalogPort catalog, UpdateFunctionalityStateRepository repository) {
            super(catalog, change -> repository.existsInApplication(change.functionalityId(), change.applicationId()),
                    FUN_226, NotFoundException::buildUserException);
        }
    }

    private static final class StateRule extends RuleValidator<FunctionalityStateChange> {
        private StateRule(CatalogPort catalog, UpdateFunctionalityStateRepository repository) {
            super(catalog, change -> repository.findStateNameById(change.stateId())
                    .filter(name -> isActive(name) || isInactive(name)).isPresent(), FUN_185);
        }
    }
}
