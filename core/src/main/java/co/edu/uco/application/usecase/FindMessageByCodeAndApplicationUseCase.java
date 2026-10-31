package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.usecase.domain.MessageDomain;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.handling.HandlingFindMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class FindMessageByCodeAndApplicationUseCase implements HandlingFindMessageByCodeAndApplicationPort {
    private final MessageCatalogStrategy messageCatalogStrategy;
    private final FindMessageCodeValidator findMessageCodeValidator;
    private final DataMapper<MessageData, MessageDomain, MessageDTO> entityMapper;
    private final MessageEnvironmentResolver environmentResolver;
    private final LoggingPort log;

    public FindMessageByCodeAndApplicationUseCase(MessageCatalogStrategy messageCatalogStrategy, FindMessageCodeValidator findMessageCodeValidator,
                                                   DataMapper<MessageData, MessageDomain, MessageDTO> entityMapper,
                                                   MessageEnvironmentResolver environmentResolver,
                                                   LoggingPortFactory loggerFactory) {
        this.messageCatalogStrategy = messageCatalogStrategy;
        this.findMessageCodeValidator = findMessageCodeValidator;
        this.entityMapper = entityMapper;
        this.environmentResolver = environmentResolver;
        this.log = loggerFactory.getLogger(FindMessageByCodeAndApplicationUseCase.class);
    }
    @Override
    public MessageDTO execute(String messageCode, MessageAccessContext context) {
        String applicationId = null;
        try {
            findMessageCodeValidator.validate(messageCode);
            applicationId = environmentResolver.resolveApplication(context);
            var messageDataOptional = messageCatalogStrategy
                    .getMessageByCodeAndApplication(messageCode, applicationId);
            if (messageDataOptional.isEmpty()) {
                var errorMessage = String.format(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()), messageCode, applicationId);
                throw BusinessException.buildUserException(errorMessage);
            }
            return entityMapper.mapperDTO(messageDataOptional.get());
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (Exception exception) {
            String errorMessage = String.format(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()), messageCode, applicationId);
            log.error(errorMessage, exception);
            throw BusinessException.buildUserException(errorMessage);
        }
    }
}
