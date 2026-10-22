package co.edu.uco.application.primaryports.facade.workspace.impl;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.application.usecase.PersonalWorkspaceUseCase;
import co.edu.uco.application.usecase.domain.security.WorkspaceSession;

import java.util.List;

public final class PersonalWorkspaceUseCaseFacadeImpl implements PersonalWorkspaceUseCaseFacade {
    private final PersonalWorkspaceUseCase useCase;

    public PersonalWorkspaceUseCaseFacadeImpl(PersonalWorkspaceUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public WorkspaceSession openSession(String session) {
        return useCase.openSession(session);
    }

    @Override
    public List<CatalogItemDTO> findOrganizations(WorkspaceUserDTO user) {
        return useCase.findOrganizations(user);
    }

    @Override
    public CatalogItemDTO createOrganization(CreateOrganizationDTO dto, WorkspaceUserDTO user) {
        return useCase.createOrganization(dto, user);
    }

    @Override
    public List<CatalogItemDTO> findApplications(String organizationId, WorkspaceUserDTO user) {
        return useCase.findApplications(organizationId, user);
    }

    @Override
    public void createApplication(CreateApplicationDTO dto, WorkspaceUserDTO user) {
        useCase.createApplication(dto, user);
    }
}
