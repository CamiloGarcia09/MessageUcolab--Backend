package co.edu.uco.infraestructure.config;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.impl.FindMessageByCodeAndApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.message.impl.FindMessagesByApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.message.impl.TranslateMessageByCodeAndApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.page.impl.SimplePageFacadeImpl;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.FindMessageByCodeAndApplicationUseCase;
import co.edu.uco.application.usecase.FindMessagesByApplicationUseCase;
import co.edu.uco.application.usecase.TranslateMessageByCodeAndApplicationUseCase;
import co.edu.uco.application.usecase.handling.HandlingFindMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingFindMessagesByApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingTranslateMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.application.usecase.validator.message.ListMessageValidator;
import co.edu.uco.application.usecase.validator.message.TargetLanguageValidator;
import co.edu.uco.application.usecase.validator.page.PageRequestRangeValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class MessageAdministrationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MessageAdministrationConfig.class)
            .withBean(MessageCatalogStrategy.class, () -> mock(MessageCatalogStrategy.class))
            .withBean(DeleteMessageRepository.class, () -> mock(DeleteMessageRepository.class))
            .withBean(DataBaseMessageRepository.class, () -> mock(DataBaseMessageRepository.class))
            .withBean(CatalogPort.class, () -> mock(CatalogPort.class))
            .withBean(DataMapper.class, () -> mock(DataMapper.class))
            .withBean(ListMessageValidator.class, () -> mock(ListMessageValidator.class))
            .withBean(PageRequestRangeValidator.class, () -> mock(PageRequestRangeValidator.class))
            .withBean(MessageEnvironmentResolver.class, () -> mock(MessageEnvironmentResolver.class))
            .withBean(LoggingPortFactory.class, () -> mock(LoggingPortFactory.class))
            .withBean(SimplePageFacadeImpl.class, () -> mock(SimplePageFacadeImpl.class))
            .withBean(FindMessageCodeValidator.class, () -> mock(FindMessageCodeValidator.class))
            .withBean(TargetLanguageValidator.class, () -> mock(TargetLanguageValidator.class))
            .withBean(MessageTranslationPort.class, () -> mock(MessageTranslationPort.class))
            .withBean(MessageTranslationCachePort.class, () -> mock(MessageTranslationCachePort.class));

    @Test
    void registersOneUseCaseAndFacadeForEachApplicationScopedMessageOperation() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertAll(
                    () -> assertThat(context).hasSingleBean(HandlingFindMessagesByApplicationPort.class),
                    () -> assertThat(context).hasSingleBean(FindMessagesByApplicationUseCaseFacade.class),
                    () -> assertThat(context.getBean(HandlingFindMessagesByApplicationPort.class))
                            .isInstanceOf(FindMessagesByApplicationUseCase.class),
                    () -> assertThat(context.getBean(FindMessagesByApplicationUseCaseFacade.class))
                            .isInstanceOf(FindMessagesByApplicationUseCaseFacadeImpl.class),
                    () -> assertThat(context).hasSingleBean(HandlingFindMessageByCodeAndApplicationPort.class),
                    () -> assertThat(context).hasSingleBean(FindMessageByCodeAndApplicationUseCaseFacade.class),
                    () -> assertThat(context.getBean(HandlingFindMessageByCodeAndApplicationPort.class))
                            .isInstanceOf(FindMessageByCodeAndApplicationUseCase.class),
                    () -> assertThat(context.getBean(FindMessageByCodeAndApplicationUseCaseFacade.class))
                            .isInstanceOf(FindMessageByCodeAndApplicationUseCaseFacadeImpl.class),
                    () -> assertThat(context).hasSingleBean(HandlingTranslateMessageByCodeAndApplicationPort.class),
                    () -> assertThat(context).hasSingleBean(TranslateMessageByCodeAndApplicationUseCaseFacade.class),
                    () -> assertThat(context.getBean(HandlingTranslateMessageByCodeAndApplicationPort.class))
                            .isInstanceOf(TranslateMessageByCodeAndApplicationUseCase.class),
                    () -> assertThat(context.getBean(TranslateMessageByCodeAndApplicationUseCaseFacade.class))
                            .isInstanceOf(TranslateMessageByCodeAndApplicationUseCaseFacadeImpl.class));
        });
    }

    @Test
    void coreImplementationsAreNotComponentScannedAlongsideConfigurationBeans() {
        assertAll(
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        FindMessagesByApplicationUseCase.class, Component.class)).isFalse(),
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        FindMessagesByApplicationUseCaseFacadeImpl.class, Component.class)).isFalse(),
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        FindMessageByCodeAndApplicationUseCase.class, Component.class)).isFalse(),
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        FindMessageByCodeAndApplicationUseCaseFacadeImpl.class, Component.class)).isFalse(),
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        TranslateMessageByCodeAndApplicationUseCase.class, Component.class)).isFalse(),
                () -> assertThat(AnnotatedElementUtils.hasAnnotation(
                        TranslateMessageByCodeAndApplicationUseCaseFacadeImpl.class, Component.class)).isFalse());
    }

    @Test
    void failsContextRefresh_whenRequiredPortDependencyIsAbsent() {
        new ApplicationContextRunner()
                .withUserConfiguration(MessageAdministrationConfig.class)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }
}
