package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.usecase.domain.MessageDomain;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.handling.HandlingFindMessagesByApplicationPort;
import co.edu.uco.application.usecase.validator.message.ListMessageValidator;
import co.edu.uco.application.usecase.validator.page.PageRequestRangeValidator;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;

public final class FindMessagesByApplicationUseCase implements HandlingFindMessagesByApplicationPort {
    private final MessageCatalogStrategy messageCatalogStrategy;
    private final DataMapper<MessageData, MessageDomain,MessageDTO> entityMapper;
    private final ListMessageValidator listMessageValidator;
    private final PageRequestRangeValidator rangeValidator;
    private final MessageEnvironmentResolver environmentResolver;
    private final LoggingPort log;

    public FindMessagesByApplicationUseCase(MessageCatalogStrategy messageCatalogStrategy, DataMapper<MessageData, MessageDomain, MessageDTO> entityMapper,
                                            ListMessageValidator listMessageValidator, PageRequestRangeValidator rangeValidator,
                                            MessageEnvironmentResolver environmentResolver,
                                            LoggingPortFactory loggerFactory) {
        this.messageCatalogStrategy = messageCatalogStrategy;
        this.entityMapper = entityMapper;
        this.listMessageValidator = listMessageValidator;
        this.rangeValidator = rangeValidator;
        this.environmentResolver = environmentResolver;
        this.log = loggerFactory.getLogger(FindMessagesByApplicationUseCase.class);
    }
    @Override
    public SimplePage<MessageDTO> execute(MessageAccessContext context, SimplePageRequest pageRequest) {
        try {
            listMessageValidator.validate(pageRequest);
            // Mensajes de la aplicacion; el filtro de ambientes viene en la peticion.
            var applicationId = environmentResolver.resolveApplication(context);
            var page = messageCatalogStrategy.getMessagesWithApplication(applicationId, pageRequest);
            // Una aplicacion o un filtro sin mensajes devuelve una pagina vacia, no un error.
            if (page.getTotalItems() > 0) {
                rangeValidator.validate(pageRequest.getPage(), page.getTotalPages());
            }
            var messages = page.getData().stream().map(entityMapper::mapperDTO).toList();
            return SimplePage.of(messages, page.getPage(), page.getSize(),page.getTotalItems(), page.getTotalPages());
        } catch (CrossWordsException exception) {
            throw exception;
        }
        catch (Exception exception){
            var errorMessage = messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_011.getCode());
            log.error(errorMessage);
            throw BusinessException.buildUserException(errorMessage);
        }
    }
}
