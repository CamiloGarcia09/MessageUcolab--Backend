package co.edu.uco.application.usecase.validator.application;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_152;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_050;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_231;

public final class DeleteApplicationCompositeValidator
        extends CompositeValidator<DeleteApplicationCompositeValidator.Context> {
    public record Context(String applicationId, WorkspaceUserDTO user) { }

    public DeleteApplicationCompositeValidator(CatalogPort catalog, DeleteApplicationRepository repository) {
        super(List.of(new UserRequiredRule(catalog), new ApplicationIdRule(catalog),
                new ApplicationOwnerRule(catalog, repository)), catalog);
    }

    private static final class UserRequiredRule extends RuleValidator<Context> {
        private UserRequiredRule(CatalogPort catalog) {
            super(catalog, context -> context.user() != null && context.user().id() != null,
                    FUN_152, UnauthorizedException::buildUserException);
        }
    }

    private static final class ApplicationIdRule extends RuleValidator<Context> {
        private ApplicationIdRule(CatalogPort catalog) {
            super(catalog, context -> new ValidUuidSpecification().isSatisfiedBy(context.applicationId()), FUN_050);
        }
    }

    private static final class ApplicationOwnerRule extends RuleValidator<Context> {
        private ApplicationOwnerRule(CatalogPort catalog, DeleteApplicationRepository repository) {
            super(catalog, context -> repository.ownsApplication(context.user().id(),
                    UUID.fromString(context.applicationId())), FUN_231, ForbiddenException::buildUserException);
        }
    }
}
