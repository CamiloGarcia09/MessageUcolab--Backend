package co.edu.uco.application.primaryports.facade.organization.impl;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.facade.organization.CreateOrganizationUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingCreateOrganizationPort;

public final class CreateOrganizationUseCaseFacadeImpl implements CreateOrganizationUseCaseFacade {

    private final HandlingCreateOrganizationPort handlingCreateOrganizationPort;

    public CreateOrganizationUseCaseFacadeImpl(final HandlingCreateOrganizationPort handlingCreateOrganizationPort) {
        this.handlingCreateOrganizationPort = handlingCreateOrganizationPort;
    }

    @Override
    public void execute(final CreateOrganizationDTO createOrganizationDTO) {
        handlingCreateOrganizationPort.createOrganization(createOrganizationDTO);
    }
}
