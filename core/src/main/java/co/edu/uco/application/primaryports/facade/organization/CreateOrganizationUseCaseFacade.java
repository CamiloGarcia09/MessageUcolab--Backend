package co.edu.uco.application.primaryports.facade.organization;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;

public interface CreateOrganizationUseCaseFacade {
    void execute(CreateOrganizationDTO createOrganizationDTO);
}
