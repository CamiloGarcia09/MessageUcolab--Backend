package co.edu.uco.application.usecase.validator.workspace;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;

public record WorkspaceOwnershipContext(WorkspaceUserDTO user, String organizationId) {
}
