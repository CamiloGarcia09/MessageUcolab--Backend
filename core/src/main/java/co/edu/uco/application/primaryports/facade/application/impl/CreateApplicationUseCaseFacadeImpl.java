package co.edu.uco.application.primaryports.facade.application.impl;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.facade.application.CreateApplicationUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;

public final class CreateApplicationUseCaseFacadeImpl implements CreateApplicationUseCaseFacade {

    private final HandlingCreateApplicationPort handlingCreateApplicationPort;

    public CreateApplicationUseCaseFacadeImpl(HandlingCreateApplicationPort handlingCreateApplicationPort) {
        this.handlingCreateApplicationPort = handlingCreateApplicationPort;
    }

    @Override
    public void execute(CreateApplicationDTO createApplicationDTO) {
        handlingCreateApplicationPort.createApplication(createApplicationDTO);
    }
}
