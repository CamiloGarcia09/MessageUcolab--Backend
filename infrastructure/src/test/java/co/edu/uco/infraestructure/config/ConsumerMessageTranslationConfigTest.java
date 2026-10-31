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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConsumerMessageTranslationConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConsumerMessageTranslationConfig.class)
            .withBean(MessageTranslationCachePort.class, () -> mock(MessageTranslationCachePort.class))
            .withBean(MessageTranslationPort.class, () -> mock(MessageTranslationPort.class))
            .withBean(CatalogPort.class, () -> mock(CatalogPort.class))
            .withBean(ResolveMessageUseCaseFacade.class, () -> mock(ResolveMessageUseCaseFacade.class))
            .withBean(ApplicationLanguageRepository.class, () -> mock(ApplicationLanguageRepository.class));

    @Test
    void registersStrategyValidatorsAndConsumerFacade() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed()
                    .hasSingleBean(ConsumerMessageTranslationStrategy.class)
                    .hasSingleBean(ConsumerTranslationLanguageValidator.class)
                    .hasSingleBean(ApplicationBaseLanguageValidator.class)
                    .hasSingleBean(ConsumerTranslationConsistencyValidator.class)
                    .hasSingleBean(TranslateConsumerMessageUseCaseFacade.class);
            assertThat(context.getBean(TranslateConsumerMessageUseCaseFacade.class))
                    .isInstanceOf(TranslateConsumerMessageUseCase.class);
        });
    }
}
