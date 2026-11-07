package co.edu.uco.application.primaryports.facade.functionality.impl;

import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import co.edu.uco.application.primaryports.facade.functionality.UpdateFunctionalityStateUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingUpdateFunctionalityStatePort;

public final class UpdateFunctionalityStateUseCaseFacadeImpl implements UpdateFunctionalityStateUseCaseFacade {
    private final HandlingUpdateFunctionalityStatePort port;

    public UpdateFunctionalityStateUseCaseFacadeImpl(HandlingUpdateFunctionalityStatePort port) {
        this.port = port;
    }

    @Override
    public void execute(String functionalityId, UpdateFunctionalityStateDTO dto) {
        port.updateState(functionalityId, dto);
    }
}
