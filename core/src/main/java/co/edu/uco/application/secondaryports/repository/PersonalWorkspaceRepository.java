package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;

import java.util.List;
import java.util.UUID;

public interface PersonalWorkspaceRepository {
    void saveUser(UUID userId, SessionIdentity identity);

    List<CatalogItemDTO> findOrganizations(UUID userId);

    List<CatalogItemDTO> findApplications(UUID userId);

    List<CatalogItemDTO> findApplicationsByOrganization(UUID userId, UUID organizationId);

    boolean ownsOrganization(UUID userId, UUID organizationId);

    boolean organizationNameExists(UUID userId, String name);

    void createOrganization(OrganizationEntity organization, UUID userId);
}
