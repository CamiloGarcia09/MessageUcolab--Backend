package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingUpdateMessagePort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.UpdateMessageCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import java.util.List;
import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isActive;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_095;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_072;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_197;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_206;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_207;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_208;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getStringFromUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class UpdateMessageUseCase implements HandlingUpdateMessagePort {

    private final UpdateMessageRepository updateMessageRepository;
    private final DataBaseMessageRepository messageRepository;
    private final UpdateMessageCompositeValidator validator;
    private final MessageEnvironmentResolver environmentResolver;
    private final MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository;
    private final MessageCatalogStrategy messageCatalogStrategy;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public UpdateMessageUseCase(
            UpdateMessageRepository updateMessageRepository,
            DataBaseMessageRepository messageRepository,
            UpdateMessageCompositeValidator validator,
            MessageEnvironmentResolver environmentResolver,
            MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository,
            MessageCatalogStrategy messageCatalogStrategy,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory) {
        this.updateMessageRepository = updateMessageRepository;
        this.messageRepository = messageRepository;
        this.validator = validator;
        this.environmentResolver = environmentResolver;
        this.messageEnvironmentStateRepository = messageEnvironmentStateRepository;
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

        var applicationId = environmentResolver.resolveApplication(context);

        // El codigo es unico por aplicacion: nunca se busca en otras aplicaciones.
        var existing = messageRepository.findByCodeAndApplication(sanitizedId, applicationId)
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_208.getCode())));

        validator.validate(dto, applicationId);

        List<String> environmentIds = dto.getEnvironmentIds() == null ? null : dto.getEnvironmentIds().stream()
                .map(environmentId -> getUUIDFromString(environmentId).toString())
                .toList();
        var messageEnvironmentStateId = environmentIds == null ? null : activeMessageEnvironmentStateId();

        try {
            if (dto.isEnvironmentsOnly()) {
                updateMessageRepository.updateEnvironments(existing.getId().toString(), environmentIds,
                        messageEnvironmentStateId);
            } else {
                updateMessageRepository.updateMessage(messageData(existing, dto, applicationId), environmentIds,
                        messageEnvironmentStateId);
            }
            messageCatalogStrategy.evictMessage(existing.getCode());
            log.info(catalogPort.getMessage(TCH_095.getCode()), sanitizedId);
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(TCH_072.getCode()), ex);
            throw CrossWordsException.build(catalogPort.getMessage(FUN_207.getCode()), ex);
        }
    }

    private MessageData messageData(MessageData existing, UpdateMessageDTO dto, String applicationId) {
        var message = new MessageData(existing.getId(), existing.getCode(), dto.getTitle(), dto.getContent(),
                new MessageTypeData(UtilUUID.getStringToUUID(dto.getTypeId()), EMPTY),
                new MessageCategoryData(UtilUUID.getStringToUUID(dto.getCategoryId()), EMPTY), EMPTY,
                new FunctionalityData(UtilUUID.getStringToUUID(dto.getFunctionalityId()), EMPTY,
                        ApplicationData.build(UtilUUID.getStringToUUID(applicationId), EMPTY)));
        message.setStatus(new StatusMessageData(UtilUUID.getStringToUUID(dto.getStatusId()), EMPTY));
        return message;
    }

    private String activeMessageEnvironmentStateId() {
        var states = messageEnvironmentStateRepository.findAll();
        return (states == null ? List.<MessageEnvironmentStateData>of() : states)
                .stream()
                .filter(state -> state != null && isActive(state.getName()))
                .map(state -> getStringFromUUID(state.getId()))
                .findFirst()
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_197.getCode())));
    }
}
