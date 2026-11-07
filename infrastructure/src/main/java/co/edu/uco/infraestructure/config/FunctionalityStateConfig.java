package co.edu.uco.infraestructure.config;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.facade.functionality.UpdateFunctionalityStateUseCaseFacade;
import co.edu.uco.application.primaryports.facade.functionality.impl.UpdateFunctionalityStateUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.application.usecase.UpdateFunctionalityStateUseCase;
import co.edu.uco.application.usecase.validator.functionality.UpdateFunctionalityStateCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionalityStateConfig {
    @Bean
    UpdateFunctionalityStateCompositeValidator updateFunctionalityStateValidator(CatalogPort catalog,
            UpdateFunctionalityStateRepository repository) {
        return new UpdateFunctionalityStateCompositeValidator(catalog, repository);
    }

    @Bean
    UpdateFunctionalityStateUseCase updateFunctionalityStateUseCase(UpdateFunctionalityStateRepository repository,
            MessageStateCatalogRepository states, UpdateFunctionalityStateCompositeValidator validator,
            MessageCatalogStrategy messages, CatalogPort catalog) {
        return new UpdateFunctionalityStateUseCase(repository, states, validator, messages, catalog);
    }

    @Bean
    UpdateFunctionalityStateUseCaseFacade updateFunctionalityStateFacade(UpdateFunctionalityStateUseCase useCase) {
        return new UpdateFunctionalityStateUseCaseFacadeImpl(useCase);
    }
}
