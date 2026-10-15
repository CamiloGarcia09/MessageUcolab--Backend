package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingUpdateMessagePort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.UpdateMessageCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import org.springframework.stereotype.Component;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_035;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_206;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_207;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_208;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getStringFromUUID;

@Component
public final class UpdateMessageUseCase implements HandlingUpdateMessagePort {

    private final UpdateMessageRepository updateMessageRepository;
    private final DataBaseMessageRepository messageRepository;
    private final UpdateMessageCompositeValidator validator;
    private final MessageEnvironmentResolver environmentResolver;
    private final EnvironmentRepository environmentRepository;
    private final MessageCatalogStrategy messageCatalogStrategy;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public UpdateMessageUseCase(
            UpdateMessageRepository updateMessageRepository,
            DataBaseMessageRepository messageRepository,
            UpdateMessageCompositeValidator validator,
            MessageEnvironmentResolver environmentResolver,
            EnvironmentRepository environmentRepository,
            MessageCatalogStrategy messageCatalogStrategy,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory) {
        this.updateMessageRepository = updateMessageRepository;
        this.messageRepository = messageRepository;
        this.validator = validator;
        this.environmentResolver = environmentResolver;
        this.environmentRepository = environmentRepository;
        this.messageCatalogStrategy = messageCatalogStrategy;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(UpdateMessageUseCase.class);
    }

    @Override
    public void updateMessage(String messageId, UpdateMessageDTO dto, MessageAccessContext context) {
        var sanitizedId = trim(messageId);
        if (sanitizedId.isBlank()) {
            throw BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_206.getCode()));
        }

        var authenticatedEnvironmentId =
                environmentResolver.resolve(context, PermissionCode.MESSAGE_UPDATE);

        var applicationId = environmentRepository.findById(authenticatedEnvironmentId)
                .map(environment -> getStringFromUUID(environment.getApplication().getId()))
                .orElseThrow(() -> ForbiddenException.buildUserException(catalogPort.getMessage(FUN_035.getCode())));

        var existing = messageRepository.findByCode(sanitizedId)
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_208.getCode())));

        validator.validate(dto, applicationId);

        try {
            var messageData = new MessageData(
                    existing.getId(),
                    existing.getCode(),
                    dto.getTitle(),
                    dto.getContent(),
                    new MessageTypeData(UtilUUID.getStringToUUID(dto.getTypeId()), EMPTY),
                    new MessageCategoryData(UtilUUID.getStringToUUID(dto.getCategoryId()), EMPTY),
                    EMPTY,
                    new FunctionalityData(
                            UtilUUID.getStringToUUID(dto.getFunctionalityId()),
                            EMPTY,
                            ApplicationData.build(UtilUUID.getStringToUUID(applicationId), EMPTY)
                    )
            );
            messageData.setStatus(new StatusMessageData(UtilUUID.getStringToUUID(dto.getStatusId()), EMPTY));

            updateMessageRepository.updateMessage(messageData);
            messageCatalogStrategy.evictMessage(existing.getCode());
            log.info("Message updated successfully with id: {}", sanitizedId);
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating message in repository", ex);
            throw CrossWordsException.build(catalogPort.getMessage(FUN_207.getCode()), ex);
        }
    }
}
