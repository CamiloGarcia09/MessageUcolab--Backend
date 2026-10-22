package co.edu.uco.application.usecase.validator.workspace;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;

public final class WorkspaceUserCompositeValidator extends CompositeValidator<WorkspaceUserDTO> {
    public WorkspaceUserCompositeValidator(CatalogPort catalog) {
        super(List.of(new UserIdRule(catalog)), catalog);
    }

    private static final class UserIdRule extends RuleValidator<WorkspaceUserDTO> {
        private UserIdRule(CatalogPort catalog) {
            super(catalog, user -> user.id() != null, MessageCatalogCodeEnum.FUN_152,
                    UnauthorizedException::buildUserException);
        }
    }
}
