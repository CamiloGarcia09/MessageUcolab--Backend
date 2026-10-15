package co.edu.uco.application.usecase;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.DeleteMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingDeleteMessagePort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.springframework.stereotype.Component;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_206;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_208;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public final class DeleteMessageUseCase implements HandlingDeleteMessagePort {

    private final DeleteMessageRepository deleteMessageRepository;
    private final DataBaseMessageRepository messageRepository;
    private final MessageEnvironmentResolver environmentResolver;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public DeleteMessageUseCase(
            DeleteMessageRepository deleteMessageRepository,
            DataBaseMessageRepository messageRepository,
            MessageEnvironmentResolver environmentResolver,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory) {
        this.deleteMessageRepository = deleteMessageRepository;
        this.messageRepository = messageRepository;
        this.environmentResolver = environmentResolver;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(DeleteMessageUseCase.class);
    }

    @Override
    public void deleteMessage(String messageId, MessageAccessContext context) {
        var sanitizedId = trim(messageId);
        if (sanitizedId.isBlank()) {
            throw BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_206.getCode()));
        }

        environmentResolver.resolve(context, PermissionCode.MESSAGE_DELETE);

        var existing = messageRepository.findByCode(sanitizedId)
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_208.getCode())));

        try {
            deleteMessageRepository.deleteMessage(existing.getId().toString());
            log.info("Message deleted successfully with id: {}", sanitizedId);
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting message in repository", ex);
            throw CrossWordsException.build(catalogPort.getMessage(FUN_208.getCode()), ex);
        }
    }
}
