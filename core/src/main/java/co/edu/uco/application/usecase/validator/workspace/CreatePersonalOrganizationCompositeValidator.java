package co.edu.uco.application.usecase.validator.workspace;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.PersonalWorkspaceRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.Validator;
import co.edu.uco.application.usecase.validator.organization.rule.OrganizationNameMaxLengthRule;
import co.edu.uco.application.usecase.validator.organization.rule.OrganizationNameRequiredRule;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;

public final class CreatePersonalOrganizationCompositeValidator
        extends CompositeValidator<CreatePersonalOrganizationCompositeValidator.Context> {
    public record Context(WorkspaceUserDTO user, CreateOrganizationDTO organization) {
    }

    public CreatePersonalOrganizationCompositeValidator(CatalogPort catalogPort,
                                                         PersonalWorkspaceRepository repository) {
        super(List.of(new UserRequiredRule(catalogPort), new OrganizationDataRule(catalogPort),
                new OrganizationNameRule(catalogPort), new UniqueNameRule(catalogPort, repository)), catalogPort);
    }

    private static final class UserRequiredRule extends RuleValidator<Context> {
        private UserRequiredRule(CatalogPort catalogPort) {
            super(catalogPort, context -> context.user() != null && context.user().id() != null,
                    MessageCatalogCodeEnum.FUN_152, UnauthorizedException::buildUserException);
        }
    }

    private static final class OrganizationDataRule extends RuleValidator<Context> {
        private OrganizationDataRule(CatalogPort catalogPort) {
            super(catalogPort, context -> context.organization() != null, MessageCatalogCodeEnum.FUN_010);
        }
    }

    private static final class OrganizationNameRule implements Validator<Context> {
        private final CompositeValidator<CreateOrganizationDTO> validator;

        private OrganizationNameRule(CatalogPort catalogPort) {
            validator = new CompositeValidator<>(List.of(new OrganizationNameRequiredRule(catalogPort),
                    new OrganizationNameMaxLengthRule(catalogPort)), catalogPort);
        }

        @Override
        public void validate(Context context) {
            validator.validate(context.organization());
        }
    }

    private static final class UniqueNameRule extends RuleValidator<Context> {
        private UniqueNameRule(CatalogPort catalogPort, PersonalWorkspaceRepository repository) {
            super(catalogPort, context -> !repository.organizationNameExists(context.user().id(),
                            context.organization().getName()),
                    MessageCatalogCodeEnum.FUN_149, ConflictException::buildUserException);
        }
    }
}
