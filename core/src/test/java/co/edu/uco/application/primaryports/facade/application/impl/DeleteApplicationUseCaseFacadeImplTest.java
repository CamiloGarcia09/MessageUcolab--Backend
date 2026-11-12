package co.edu.uco.application.primaryports.facade.application.impl;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.usecase.DeleteApplicationUseCase;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationUseCaseFacadeImplTest {
    @Mock DeleteApplicationUseCase useCase;

    @Test
    void execute_forwardsApplicationAndAuthenticatedUserToUseCase() {
        var applicationId = UUID.randomUUID().toString();
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");

        new DeleteApplicationUseCaseFacadeImpl(useCase).execute(applicationId, user);

        verify(useCase).execute(applicationId, user);
    }

    @Test
    void execute_propagatesUseCaseDomainFailure() {
        var failure = UnauthorizedException.buildUserException("unauthorized");
        doThrow(failure).when(useCase).execute("invalid", null);

        assertThatThrownBy(() -> new DeleteApplicationUseCaseFacadeImpl(useCase).execute("invalid", null))
                .isSameAs(failure);
    }
}
