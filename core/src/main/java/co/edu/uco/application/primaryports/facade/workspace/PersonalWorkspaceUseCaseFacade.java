package co.edu.uco.application.primaryports.facade.workspace;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.usecase.domain.security.WorkspaceSession;

import java.util.List;

public interface PersonalWorkspaceUseCaseFacade {
    WorkspaceSession openSession(String session);

    List<CatalogItemDTO> findOrganizations(WorkspaceUserDTO user);

    CatalogItemDTO createOrganization(CreateOrganizationDTO dto, WorkspaceUserDTO user);

    List<CatalogItemDTO> findApplications(String organizationId, WorkspaceUserDTO user);

    void createApplication(CreateApplicationDTO dto, WorkspaceUserDTO user);
}
