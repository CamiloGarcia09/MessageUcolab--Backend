package co.edu.uco.application.usecase.domain.security;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;

import java.util.List;

public record WorkspaceSession(WorkspaceUserDTO user, List<CatalogItemDTO> applications) {
    public WorkspaceSession {
        applications = List.copyOf(applications);
    }
}
