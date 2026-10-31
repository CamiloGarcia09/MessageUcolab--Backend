package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;

public interface HandlingCreateOrganizationPort {
    void createOrganization(CreateOrganizationDTO organizationDTO);
}
