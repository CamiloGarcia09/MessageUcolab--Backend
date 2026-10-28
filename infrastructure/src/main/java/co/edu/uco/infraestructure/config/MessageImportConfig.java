package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.*;
import co.edu.uco.application.usecase.MessageImportUseCase;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.message.importing.ImportConfirmationCompositeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageImportConfig {
    @Bean
    public MessageImportUseCase messageImportUseCase(CatalogPort catalog, MessageEnvironmentResolver access,
            MessageEnvironmentSelectionCompositeValidator environments, FunctionalityCatalogRepository functionalities,
            MessageTypeCatalogRepository types, MessageCategoryCatalogRepository categories,
            MessageStateCatalogRepository states, MessageEnvironmentStateCatalogRepository environmentStates,
            MessageCodeQueryPort codes, MessageImportDraftStore drafts, MessageImportRepository repository,
            EnvironmentRepository environmentRepository) {
        return new MessageImportUseCase(catalog, access, environments, functionalities, types, categories,
                states, environmentStates, codes, drafts, repository,
                new ImportConfirmationCompositeValidator(catalog, environmentRepository));
    }
}
