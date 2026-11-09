package co.edu.uco.application.primaryports.facade.application;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;

public interface DeleteApplicationUseCaseFacade {
    void execute(String applicationId, WorkspaceUserDTO user);
}
