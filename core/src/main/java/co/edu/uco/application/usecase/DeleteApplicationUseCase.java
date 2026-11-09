package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.secondaryports.cache.ApplicationCachePort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.application.usecase.validator.application.DeleteApplicationCompositeValidator;

import java.util.UUID;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_108;

public final class DeleteApplicationUseCase {
    private final DeleteApplicationRepository repository;
    private final DeleteApplicationCompositeValidator validator;
    private final ApplicationCachePort cache;
    private final CatalogPort catalog;
    private final LoggingPort log;

    public DeleteApplicationUseCase(DeleteApplicationRepository repository,
            DeleteApplicationCompositeValidator validator, ApplicationCachePort cache,
            CatalogPort catalog, LoggingPortFactory logging) {
        this.repository = repository;
        this.validator = validator;
        this.cache = cache;
        this.catalog = catalog;
        this.log = logging.getLogger(DeleteApplicationUseCase.class);
    }

    public void execute(String applicationId, WorkspaceUserDTO user) {
        validator.validate(new DeleteApplicationCompositeValidator.Context(applicationId, user));
        var id = UUID.fromString(applicationId);
        var deletedMessages = repository.deleteAtomically(user.id(), id);
        try {
            cache.evict(id, deletedMessages);
        } catch (RuntimeException exception) {
            // The database commit has succeeded. Never report a failed deletion because Redis is unavailable.
            // All reads revalidate the application in the database before accessing these short-lived caches.
            log.error(catalog.getMessage(TCH_108.getCode()), exception);
        }
    }
}
