package co.edu.uco.application.usecase.validator.workspace;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.PersonalWorkspaceRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;
import java.util.UUID;

public final class WorkspaceOwnershipCompositeValidator extends CompositeValidator<WorkspaceOwnershipContext> {
    public WorkspaceOwnershipCompositeValidator(CatalogPort catalogPort, PersonalWorkspaceRepository repository) {
        super(List.of(new UserRequiredRule(catalogPort), new OrganizationIdRule(catalogPort),
                new OrganizationOwnerRule(catalogPort, repository)), catalogPort);
    }

    private static final class UserRequiredRule extends RuleValidator<WorkspaceOwnershipContext> {
        private UserRequiredRule(CatalogPort catalogPort) {
            super(catalogPort, context -> context.user() != null && context.user().id() != null,
                    MessageCatalogCodeEnum.FUN_152, UnauthorizedException::buildUserException);
        }
    }

    private static final class OrganizationIdRule extends RuleValidator<WorkspaceOwnershipContext> {
        private OrganizationIdRule(CatalogPort catalogPort) {
            super(catalogPort, context -> new ValidUuidSpecification().isSatisfiedBy(context.organizationId()),
                    MessageCatalogCodeEnum.FUN_214);
        }
    }

    private static final class OrganizationOwnerRule extends RuleValidator<WorkspaceOwnershipContext> {
        private OrganizationOwnerRule(CatalogPort catalogPort, PersonalWorkspaceRepository repository) {
            super(catalogPort, context -> repository.ownsOrganization(context.user().id(),
                            UUID.fromString(context.organizationId())),
                    MessageCatalogCodeEnum.FUN_215, ForbiddenException::buildUserException);
        }
    }
}
