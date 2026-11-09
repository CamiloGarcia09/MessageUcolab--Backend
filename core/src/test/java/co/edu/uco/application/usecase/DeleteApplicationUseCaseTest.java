package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.cache.ApplicationCachePort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.application.usecase.validator.application.DeleteApplicationCompositeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationUseCaseTest {
    @Mock DeleteApplicationRepository repository;
    @Mock DeleteApplicationCompositeValidator validator;
    @Mock ApplicationCachePort cache;
    @Mock CatalogPort catalog;
    @Mock LoggingPortFactory loggingFactory;
    @Mock LoggingPort log;

    @Test
    void execute_validOwnedApplication_deletesAtomicallyThenEvictsItsCache() {
        var applicationId = UUID.randomUUID();
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");
        var messageIds = List.of(UUID.randomUUID());
        when(loggingFactory.getLogger(DeleteApplicationUseCase.class)).thenReturn(log);
        when(repository.deleteAtomically(user.id(), applicationId)).thenReturn(messageIds);

        new DeleteApplicationUseCase(repository, validator, cache, catalog, loggingFactory)
                .execute(applicationId.toString(), user);

        verify(validator).validate(new DeleteApplicationCompositeValidator.Context(applicationId.toString(), user));
        verify(repository).deleteAtomically(user.id(), applicationId);
        verify(cache).evict(applicationId, messageIds);
    }

    @Test
    void execute_cacheFailureAfterCommit_doesNotReportDeletionFailure() {
        var applicationId = UUID.randomUUID();
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@example.test", "Owner");
        var cacheFailure = new IllegalStateException("cache unavailable");
        when(loggingFactory.getLogger(DeleteApplicationUseCase.class)).thenReturn(log);
        when(repository.deleteAtomically(user.id(), applicationId)).thenReturn(List.of());
        doThrow(cacheFailure).when(cache).evict(applicationId, List.of());
        when(catalog.getMessage("TCH_108")).thenReturn("cache eviction failed");

        assertThatCode(() -> new DeleteApplicationUseCase(repository, validator, cache, catalog, loggingFactory)
                .execute(applicationId.toString(), user)).doesNotThrowAnyException();

        verify(log).error("cache eviction failed", cacheFailure);
        verify(repository).deleteAtomically(user.id(), applicationId);
    }
}
