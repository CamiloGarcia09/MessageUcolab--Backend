package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.repository.PersonalWorkspaceRepository;
import co.edu.uco.application.secondaryports.security.SessionIdentityPort;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.application.usecase.domain.security.WorkspaceSession;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.validator.workspace.CreatePersonalOrganizationCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.SessionIdentityCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.WorkspaceOwnershipCompositeValidator;
import co.edu.uco.application.usecase.validator.workspace.WorkspaceUserCompositeValidator;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalWorkspaceUseCase")
class PersonalWorkspaceUseCaseTest {
    private static final String SESSION = "trusted-session";
    private static final UUID ORGANIZATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock private SessionIdentityPort identityPort;
    @Mock private PersonalWorkspaceRepository repository;
    @Mock private SessionIdentityCompositeValidator identityValidator;
    @Mock private WorkspaceOwnershipCompositeValidator ownershipValidator;
    @Mock private CreatePersonalOrganizationCompositeValidator organizationValidator;
    @Mock private HandlingCreateApplicationPort createApplication;
    @Mock private WorkspaceUserCompositeValidator userValidator;

    private PersonalWorkspaceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PersonalWorkspaceUseCase(identityPort, repository, identityValidator, ownershipValidator,
                organizationValidator, createApplication, userValidator);
    }

    @Nested
    @DisplayName("openSession")
    class OpenSession {
        @Test
        @DisplayName("derives stable local identity independently of mutable email")
        void openSession_emailChanges_preservesUserIdAndListsOwnedApplications() {
            SessionIdentity firstIdentity = new SessionIdentity("idp", "tenant", "subject", "old@mail", "Name");
            SessionIdentity updatedIdentity = new SessionIdentity("idp", "tenant", "subject", "new@mail", "Name");
            when(identityPort.resolve(SESSION)).thenReturn(firstIdentity, updatedIdentity);
            when(repository.findApplications(any())).thenReturn(List.of(CatalogItemDTO.create("app", "Owned")));

            WorkspaceSession first = useCase.openSession(SESSION);
            WorkspaceSession updated = useCase.openSession(SESSION);

            assertThat(updated.user().id()).isEqualTo(first.user().id());
            assertThat(updated.user().email()).isEqualTo("new@mail");
            assertThat(updated.applications()).containsExactly(CatalogItemDTO.create("app", "Owned"));
            verify(repository).saveUser(first.user().id(), firstIdentity);
            verify(repository).saveUser(updated.user().id(), updatedIdentity);
            verify(repository, times(2)).findApplications(updated.user().id());
        }

        @Test
        @DisplayName("length-prefixes identity components to prevent ambiguous concatenations")
        void openSession_componentBoundariesDiffer_producesDifferentUserIds() {
            when(identityPort.resolve(SESSION)).thenReturn(
                    new SessionIdentity("a", "bc", "d", "same@mail", "Name"),
                    new SessionIdentity("ab", "c", "d", "same@mail", "Name"));
            when(repository.findApplications(any())).thenReturn(List.of());

            UUID firstId = useCase.openSession(SESSION).user().id();
            UUID secondId = useCase.openSession(SESSION).user().id();

            assertThat(secondId).isNotEqualTo(firstId);
            verify(repository).saveUser(firstId, new SessionIdentity("a", "bc", "d", "same@mail", "Name"));
            verify(repository).saveUser(secondId, new SessionIdentity("ab", "c", "d", "same@mail", "Name"));
        }

        @Test
        @DisplayName("rejects absent trusted identity before persisting a user")
        void openSession_identityInvalid_propagatesUnauthorizedAndDoesNotPersist() {
            when(identityPort.resolve(SESSION)).thenReturn(null);
            doThrow(UnauthorizedException.buildUserException("Unauthenticated"))
                    .when(identityValidator).validate(null);

            assertThatThrownBy(() -> useCase.openSession(SESSION)).isInstanceOf(UnauthorizedException.class);

            verify(repository, never()).saveUser(any(), any());
            verify(repository, never()).findApplications(any());
        }
    }

    @Nested
    @DisplayName("createApplication")
    class CreateApplication {
        @Test
        @DisplayName("validates organization ownership and never forwards a client-selected application ID")
        void createApplication_clientSuppliesId_delegatesWithoutThatId() {
            WorkspaceUserDTO user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@mail", "Owner");
            CreateApplicationDTO request = CreateApplicationDTO.builder().id(UUID.randomUUID().toString())
                    .name("App").organizationId(ORGANIZATION_ID.toString()).languageId("lang")
                    .stateId("state").build();
            ArgumentCaptor<CreateApplicationDTO> captor = ArgumentCaptor.forClass(CreateApplicationDTO.class);

            useCase.createApplication(request, user);

            verify(ownershipValidator).validate(any());
            verify(createApplication).createApplication(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().getOrganizationId()).isEqualTo(ORGANIZATION_ID.toString());
        }

        @Test
        @DisplayName("rejects application creation for organization not owned by the user")
        void createApplication_foreignOrganization_propagatesForbidden() {
            WorkspaceUserDTO user = new WorkspaceUserDTO(UUID.randomUUID(), "user@mail", "User");
            CreateApplicationDTO request = CreateApplicationDTO.builder().name("App")
                    .organizationId(ORGANIZATION_ID.toString()).languageId("lang").stateId("state").build();
            doThrow(ForbiddenException.buildUserException("Not owner"))
                    .when(ownershipValidator).validate(any());

            assertThatThrownBy(() -> useCase.createApplication(request, user)).isInstanceOf(ForbiddenException.class);

            verify(createApplication, never()).createApplication(any());
        }
    }

    @Nested
    @DisplayName("createOrganization")
    class CreateOrganization {
        @Test
        @DisplayName("assigns a new server-side UUID and the authenticated user as owner")
        void createOrganization_validRequest_persistsOwnerAndReturnsGeneratedId() {
            UUID ownerId = UUID.randomUUID();
            WorkspaceUserDTO user = new WorkspaceUserDTO(ownerId, "user@mail", "User");
            CreateOrganizationDTO request = CreateOrganizationDTO.builder().name("Personal").build();
            ArgumentCaptor<OrganizationEntity> captor = ArgumentCaptor.forClass(OrganizationEntity.class);

            CatalogItemDTO response = useCase.createOrganization(request, user);

            verify(repository).createOrganization(captor.capture(), org.mockito.ArgumentMatchers.eq(ownerId));
            assertThat(captor.getValue().getId()).isEqualTo(UUID.fromString(response.getId()));
            assertThat(captor.getValue().getName()).isEqualTo("Personal");
            assertThat(response.getName()).isEqualTo("Personal");
        }
    }
}
