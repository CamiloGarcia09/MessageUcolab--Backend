package co.edu.uco.application.usecase.validator.application;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationCompositeValidatorTest {
    @Mock CatalogPort catalog;
    @Mock DeleteApplicationRepository repository;

    @Test
    void validate_ownedApplication_acceptsOwnerAndBoundaryValidUuid() {
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");
        var applicationId = UUID.randomUUID();
        when(repository.ownsApplication(user.id(), applicationId)).thenReturn(true);

        new DeleteApplicationCompositeValidator(catalog, repository)
                .validate(new DeleteApplicationCompositeValidator.Context(applicationId.toString(), user));

        verify(repository).ownsApplication(user.id(), applicationId);
    }

    @Test
    void validate_missingUser_throwsUnauthorizedBeforeRepositoryAccess() {
        when(catalog.getMessage("FUN_152")).thenReturn("user required");

        assertThatThrownBy(() -> new DeleteApplicationCompositeValidator(catalog, repository)
                .validate(new DeleteApplicationCompositeValidator.Context(UUID.randomUUID().toString(), null)))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validate_invalidApplicationId_throwsValidationException() {
        when(catalog.getMessage("FUN_050")).thenReturn("invalid id");
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");

        assertThatThrownBy(() -> new DeleteApplicationCompositeValidator(catalog, repository)
                .validate(new DeleteApplicationCompositeValidator.Context("not-a-uuid", user)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validate_applicationNotOwned_throwsForbiddenException() {
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");
        var applicationId = UUID.randomUUID();
        when(repository.ownsApplication(user.id(), applicationId)).thenReturn(false);
        when(catalog.getMessage("FUN_231")).thenReturn("not owner");

        assertThatThrownBy(() -> new DeleteApplicationCompositeValidator(catalog, repository)
                .validate(new DeleteApplicationCompositeValidator.Context(applicationId.toString(), user)))
                .isInstanceOf(ForbiddenException.class);
    }
}
