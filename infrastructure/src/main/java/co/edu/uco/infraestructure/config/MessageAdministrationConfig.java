package co.edu.uco.infraestructure.config;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.impl.FindMessageByCodeAndApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.message.impl.FindMessagesByApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.message.impl.TranslateMessageByCodeAndApplicationUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.page.impl.SimplePageFacadeImpl;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.FindMessageByCodeAndApplicationUseCase;
import co.edu.uco.application.usecase.DeleteMessageUseCase;
import co.edu.uco.application.usecase.FindMessagesByApplicationUseCase;
import co.edu.uco.application.usecase.TranslateMessageByCodeAndApplicationUseCase;
import co.edu.uco.application.usecase.domain.MessageDomain;
import co.edu.uco.application.usecase.handling.HandlingFindMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import co.edu.uco.application.usecase.handling.HandlingFindMessagesByApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingTranslateMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.application.usecase.validator.message.ListMessageValidator;
import co.edu.uco.application.usecase.validator.message.TargetLanguageValidator;
import co.edu.uco.application.usecase.validator.page.PageRequestRangeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageAdministrationConfig {

    @Bean
    HandlingDeleteMessagePort handlingDeleteMessagePort(
            DeleteMessageRepository deleteRepository, DataBaseMessageRepository messageRepository,
            MessageEnvironmentResolver environmentResolver, CatalogPort catalogPort,
            LoggingPortFactory loggerFactory, MessageCatalogStrategy catalogStrategy) {
        return new DeleteMessageUseCase(deleteRepository, messageRepository, environmentResolver,
                catalogPort, loggerFactory, catalogStrategy);
    }

    @Bean
    HandlingFindMessagesByApplicationPort handlingFindMessagesByApplicationPort(
            MessageCatalogStrategy catalogStrategy, DataMapper<MessageData, MessageDomain, MessageDTO> mapper,
            ListMessageValidator listValidator, PageRequestRangeValidator rangeValidator,
            MessageEnvironmentResolver environmentResolver, LoggingPortFactory loggerFactory) {
        return new FindMessagesByApplicationUseCase(catalogStrategy, mapper, listValidator, rangeValidator,
                environmentResolver, loggerFactory);
    }

    @Bean
    FindMessagesByApplicationUseCaseFacade findMessagesByApplicationUseCaseFacade(
            HandlingFindMessagesByApplicationPort port, SimplePageFacadeImpl pageFacade) {
        return new FindMessagesByApplicationUseCaseFacadeImpl(port, pageFacade);
    }

    @Bean
    HandlingFindMessageByCodeAndApplicationPort handlingFindMessageByCodeAndApplicationPort(
            MessageCatalogStrategy catalogStrategy, FindMessageCodeValidator codeValidator,
            DataMapper<MessageData, MessageDomain, MessageDTO> mapper,
            MessageEnvironmentResolver environmentResolver, LoggingPortFactory loggerFactory) {
        return new FindMessageByCodeAndApplicationUseCase(catalogStrategy, codeValidator, mapper,
                environmentResolver, loggerFactory);
    }

    @Bean
    FindMessageByCodeAndApplicationUseCaseFacade findMessageByCodeAndApplicationUseCaseFacade(
            HandlingFindMessageByCodeAndApplicationPort port) {
        return new FindMessageByCodeAndApplicationUseCaseFacadeImpl(port);
    }

    @Bean
    HandlingTranslateMessageByCodeAndApplicationPort handlingTranslateMessageByCodeAndApplicationPort(
            MessageCatalogStrategy catalogStrategy, MessageTranslationPort translationPort,
            MessageTranslationCachePort cachePort, FindMessageCodeValidator codeValidator,
            TargetLanguageValidator languageValidator, MessageEnvironmentResolver environmentResolver,
            LoggingPortFactory loggerFactory) {
        return new TranslateMessageByCodeAndApplicationUseCase(catalogStrategy, translationPort, cachePort,
                codeValidator, languageValidator, environmentResolver, loggerFactory);
    }

    @Bean
    TranslateMessageByCodeAndApplicationUseCaseFacade translateMessageByCodeAndApplicationUseCaseFacade(
            HandlingTranslateMessageByCodeAndApplicationPort port) {
        return new TranslateMessageByCodeAndApplicationUseCaseFacadeImpl(port);
    }
}
