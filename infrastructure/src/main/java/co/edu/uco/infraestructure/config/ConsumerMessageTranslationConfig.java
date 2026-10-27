package co.edu.uco.infraestructure.config;

import co.edu.uco.application.common.catalog.strategy.ConsumerMessageTranslationStrategy;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.TranslateConsumerMessageUseCase;
import co.edu.uco.application.usecase.validator.message.ApplicationBaseLanguageValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationConsistencyValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationLanguageValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerMessageTranslationConfig {
    @Bean
    public ConsumerMessageTranslationStrategy consumerMessageTranslationStrategy(
            MessageTranslationCachePort cache, MessageTranslationPort translator, CatalogPort catalog) {
        return new ConsumerMessageTranslationStrategy(cache, translator, catalog);
    }

    @Bean
    public ConsumerTranslationLanguageValidator consumerTranslationLanguageValidator(CatalogPort catalog) {
        return new ConsumerTranslationLanguageValidator(catalog);
    }

    @Bean
    public ApplicationBaseLanguageValidator applicationBaseLanguageValidator(CatalogPort catalog) {
        return new ApplicationBaseLanguageValidator(catalog);
    }

    @Bean
    public ConsumerTranslationConsistencyValidator consumerTranslationConsistencyValidator(CatalogPort catalog) {
        return new ConsumerTranslationConsistencyValidator(catalog);
    }

    @Bean
    public TranslateConsumerMessageUseCaseFacade translateConsumerMessageUseCaseFacade(
            ResolveMessageUseCaseFacade resolver, ApplicationLanguageRepository languages,
            ConsumerMessageTranslationStrategy translations, ConsumerTranslationLanguageValidator targetValidator,
            ApplicationBaseLanguageValidator sourceValidator, ConsumerTranslationConsistencyValidator consistencyValidator) {
        return new TranslateConsumerMessageUseCase(resolver, languages, translations, targetValidator,
                sourceValidator, consistencyValidator);
    }
}
