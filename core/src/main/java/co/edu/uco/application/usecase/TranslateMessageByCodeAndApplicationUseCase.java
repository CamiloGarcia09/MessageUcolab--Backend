package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.handling.HandlingTranslateMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.application.usecase.validator.message.TargetLanguageValidator;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class TranslateMessageByCodeAndApplicationUseCase
        implements HandlingTranslateMessageByCodeAndApplicationPort {
    private static final String DEFAULT_SOURCE_LANGUAGE = "auto";

    private final MessageCatalogStrategy messageCatalogStrategy;
    private final MessageTranslationPort messageTranslationPort;
    private final MessageTranslationCachePort messageTranslationCachePort;
    private final FindMessageCodeValidator findMessageCodeValidator;
    private final TargetLanguageValidator targetLanguageValidator;
    private final MessageEnvironmentResolver environmentResolver;
    private final LoggingPort log;

    public TranslateMessageByCodeAndApplicationUseCase(
            MessageCatalogStrategy messageCatalogStrategy,
            MessageTranslationPort messageTranslationPort,
            MessageTranslationCachePort messageTranslationCachePort,
            FindMessageCodeValidator findMessageCodeValidator,
            TargetLanguageValidator targetLanguageValidator,
            MessageEnvironmentResolver environmentResolver,
            LoggingPortFactory loggerFactory
    ) {
        this.messageCatalogStrategy = messageCatalogStrategy;
        this.messageTranslationPort = messageTranslationPort;
        this.messageTranslationCachePort = messageTranslationCachePort;
        this.findMessageCodeValidator = findMessageCodeValidator;
        this.targetLanguageValidator = targetLanguageValidator;
        this.environmentResolver = environmentResolver;
        this.log = loggerFactory.getLogger(TranslateMessageByCodeAndApplicationUseCase.class);
    }

    @Override
    public TranslatedMessageDTO execute(
            String messageCode,
            MessageAccessContext context,
            String sourceLanguage,
            String targetLanguage
    ) {
        try {
            findMessageCodeValidator.validate(messageCode);
            targetLanguageValidator.validate(targetLanguage);
            // El contenido es el mismo en todos los ambientes: la traduccion se resuelve por aplicacion.
            var applicationId = environmentResolver.resolveApplication(context);

            var normalizedSourceLanguage = isEmptyOrNull(sourceLanguage)
                    ? DEFAULT_SOURCE_LANGUAGE
                    : trim(sourceLanguage);
            var messageData = messageCatalogStrategy
                    .getMessageByCodeAndApplication(messageCode, applicationId)
                    .orElseThrow(() -> {
                        var errorMessage = String.format(
                                messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()),
                                messageCode,
                                applicationId
                        );
                        return BusinessException.buildUserException(errorMessage);
                    });
            var type = messageData.getType().getName();
            var category = messageData.getCategory().getName();
            var functionality = messageData.getFunctionality().getName();
            var requestData = MessageTranslationRequestData.create(
                    messageData.getCode(),
                    normalizedSourceLanguage,
                    targetLanguage,
                    messageData.getTitle(),
                    messageData.getContent(),
                    type,
                    category,
                    messageData.getApplication(),
                    functionality
            );
            var cachedTranslation = messageTranslationCachePort.findTranslation(
                    messageData.getCode(),
                    applicationId,
                    normalizedSourceLanguage,
                    targetLanguage
            );
            if (cachedTranslation.isPresent()) {
                log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_097.getCode()),
                        messageData.getCode(), normalizedSourceLanguage, targetLanguage);
                return buildDto(messageData, normalizedSourceLanguage, targetLanguage, type, category,
                        functionality, cachedTranslation.get());
            }
            var translationData = messageTranslationPort.translate(requestData);
            messageTranslationCachePort.saveTranslation(
                    messageData.getCode(),
                    applicationId,
                    normalizedSourceLanguage,
                    targetLanguage,
                    translationData
            );
            return buildDto(messageData, normalizedSourceLanguage, targetLanguage, type, category,
                    functionality, translationData);
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (Exception exception) {
            var errorMessage = messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_048.getCode());
            log.error(errorMessage, exception);
            throw BusinessException.buildUserException(errorMessage);
        }
    }

    private TranslatedMessageDTO buildDto(
            MessageData messageData,
            String sourceLanguage,
            String targetLanguage,
            String type,
            String category,
            String functionality,
            MessageTranslationResponseData translationData
    ) {
        return TranslatedMessageDTO.create(
                messageData.getCode(),
                sourceLanguage,
                targetLanguage,
                messageData.getTitle(),
                messageData.getContent(),
                translationData.getTranslatedTitle(),
                translationData.getTranslatedContent(),
                type,
                translationData.getTranslatedType(),
                category,
                translationData.getTranslatedCategory(),
                functionality,
                translationData.getTranslatedFunctionality(),
                translationData.getProvider(),
                translationData.getModel(),
                translationData.getElapsedMillis()
        );
    }
}
