package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.application.DeleteApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.application.impl.DeleteApplicationUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.cache.ApplicationCachePort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DeleteApplicationRepository;
import co.edu.uco.application.usecase.DeleteApplicationUseCase;
import co.edu.uco.application.usecase.validator.application.DeleteApplicationCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationDeletionConfig {
    @Bean
    DeleteApplicationCompositeValidator deleteApplicationValidator(CatalogPort catalog,
            DeleteApplicationRepository repository) {
        return new DeleteApplicationCompositeValidator(catalog, repository);
    }

    @Bean
    DeleteApplicationUseCase deleteApplicationUseCase(DeleteApplicationRepository repository,
            DeleteApplicationCompositeValidator validator, ApplicationCachePort cache, CatalogPort catalog,
            LoggingPortFactory logging) {
        return new DeleteApplicationUseCase(repository, validator, cache, catalog, logging);
    }

    @Bean
    DeleteApplicationUseCaseFacade deleteApplicationFacade(DeleteApplicationUseCase useCase) {
        return new DeleteApplicationUseCaseFacadeImpl(useCase);
    }
}
