package co.edu.uco.application.primaryports.facade.application.impl;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.application.DeleteApplicationUseCaseFacade;
import co.edu.uco.application.usecase.DeleteApplicationUseCase;

public final class DeleteApplicationUseCaseFacadeImpl implements DeleteApplicationUseCaseFacade {
    private final DeleteApplicationUseCase useCase;

    public DeleteApplicationUseCaseFacadeImpl(DeleteApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void execute(String applicationId, WorkspaceUserDTO user) {
        useCase.execute(applicationId, user);
    }
}
