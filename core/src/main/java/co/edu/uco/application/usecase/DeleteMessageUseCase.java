package co.edu.uco.application.usecase;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_206;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_208;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class DeleteMessageUseCase implements HandlingDeleteMessagePort {

    private final DeleteMessageRepository deleteMessageRepository;
    private final DataBaseMessageRepository messageRepository;
    private final MessageEnvironmentResolver environmentResolver;
    private final CatalogPort catalogPort;
    private final LoggingPort log;
    private final MessageCatalogStrategy messageCatalogStrategy;

    public DeleteMessageUseCase(
            DeleteMessageRepository deleteMessageRepository,
            DataBaseMessageRepository messageRepository,
            MessageEnvironmentResolver environmentResolver,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory,
            MessageCatalogStrategy messageCatalogStrategy) {
        this.deleteMessageRepository = deleteMessageRepository;
        this.messageRepository = messageRepository;
        this.environmentResolver = environmentResolver;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(DeleteMessageUseCase.class);
        this.messageCatalogStrategy = messageCatalogStrategy;
    }

    @Override
    public void deleteMessage(String messageId, MessageAccessContext context) {
        var sanitizedId = trim(messageId);
        if (sanitizedId.isBlank()) {
            throw BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_206.getCode()));
        }

        var applicationId = environmentResolver.resolveApplication(context);

        // Borra el mensaje de todos sus ambientes; solo dentro de la aplicacion del contexto.
        var existing = messageRepository.findByCodeAndApplication(sanitizedId, applicationId)
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_208.getCode())));

        try {
            deleteMessageRepository.deleteMessage(existing.getId().toString());
            messageCatalogStrategy.evictMessage(existing.getCode());
            log.info("Message deleted successfully with id: {}", sanitizedId);
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting message in repository", ex);
            throw CrossWordsException.build(catalogPort.getMessage(FUN_208.getCode()), ex);
        }
    }
}
