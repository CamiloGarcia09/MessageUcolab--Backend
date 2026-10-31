package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.repository.PersonalWorkspaceRepository;
import co.edu.uco.application.secondaryports.security.SessionIdentityPort;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.WorkspaceSession;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.validator.workspace.CreatePersonalOrganizationCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.SessionIdentityCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.WorkspaceOwnershipCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.WorkspaceOwnershipContext;
import co.edu.uco.application.usecase.validator.workspace.WorkspaceUserCompositeValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class PersonalWorkspaceUseCase {
    private final SessionIdentityPort identityPort;
    private final PersonalWorkspaceRepository repository;
    private final SessionIdentityCompositeValidator identityValidator;
    private final WorkspaceOwnershipCompositeValidator ownershipValidator;
    private final CreatePersonalOrganizationCompositeValidator organizationValidator;
    private final HandlingCreateApplicationPort createApplication;
    private final WorkspaceUserCompositeValidator userValidator;

    public PersonalWorkspaceUseCase(SessionIdentityPort identityPort, PersonalWorkspaceRepository repository,
                                   SessionIdentityCompositeValidator identityValidator,
                                   WorkspaceOwnershipCompositeValidator ownershipValidator,
                                   CreatePersonalOrganizationCompositeValidator organizationValidator,
                                   HandlingCreateApplicationPort createApplication,
                                   WorkspaceUserCompositeValidator userValidator) {
        this.identityPort = identityPort;
        this.repository = repository;
        this.identityValidator = identityValidator;
        this.ownershipValidator = ownershipValidator;
        this.organizationValidator = organizationValidator;
        this.createApplication = createApplication;
        this.userValidator = userValidator;
    }

    public WorkspaceSession openSession(String session) {
        var identity = identityPort.resolve(session);
        identityValidator.validate(identity);
        // Length-prefixed identity parts avoid ambiguous concatenations; email is not an identity key.
        var identityKey = part(identity.source()) + part(identity.tenantId()) + part(identity.subject());
        var userId = UUID.nameUUIDFromBytes(identityKey.getBytes(StandardCharsets.UTF_8));
        repository.saveUser(userId, identity);
        var user = new WorkspaceUserDTO(userId, identity.email(), identity.name());
        return new WorkspaceSession(user, repository.findApplications(userId));
    }

    public List<CatalogItemDTO> findOrganizations(WorkspaceUserDTO user) {
        userValidator.validate(user);
        return repository.findOrganizations(user.id());
    }

    public CatalogItemDTO createOrganization(CreateOrganizationDTO dto, WorkspaceUserDTO user) {
        organizationValidator.validate(new CreatePersonalOrganizationCompositeValidator.Context(user, dto));
        var organization = new OrganizationEntity();
        organization.setId(UUID.randomUUID());
        organization.setName(dto.getName());
        repository.createOrganization(organization, user.id());
        return CatalogItemDTO.create(organization.getId().toString(), organization.getName());
    }

    public List<CatalogItemDTO> findApplications(String organizationId, WorkspaceUserDTO user) {
        ownershipValidator.validate(new WorkspaceOwnershipContext(user, organizationId));
        return repository.findApplicationsByOrganization(user.id(), UUID.fromString(organizationId));
    }

    public void createApplication(CreateApplicationDTO dto, WorkspaceUserDTO user) {
        ownershipValidator.validate(new WorkspaceOwnershipContext(user, dto == null ? null : dto.getOrganizationId()));
        // Business applications never reuse a security application id supplied by the client.
        createApplication.createApplication(CreateApplicationDTO.builder()
                .name(dto.getName()).organizationId(dto.getOrganizationId())
                .languageId(dto.getLanguageId()).stateId(dto.getStateId()).build());
    }

    private static String part(String value) {
        return value.length() + ":" + value;
    }
}
