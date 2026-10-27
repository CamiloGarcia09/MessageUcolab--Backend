package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
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
import co.edu.uco.application.secondaryports.repository.CreateMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.handling.HandlingCreateMessagePort;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import org.springframework.stereotype.Component;

import java.util.List;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_197;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilMessageCode.normalize;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getStringFromUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

@Component
public final class CreateMessageUseCase implements HandlingCreateMessagePort {

    private final CreateMessageRepository createMessageRepository;
    private final CreateMessageCompositeValidator validator;
    private final MessageEnvironmentResolver environmentResolver;
    private final MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public CreateMessageUseCase(
            CreateMessageRepository createMessageRepository,
            CreateMessageCompositeValidator validator,
            MessageEnvironmentResolver environmentResolver,
            MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory) {
        this.createMessageRepository = createMessageRepository;
        this.validator = validator;
        this.environmentResolver = environmentResolver;
        this.messageEnvironmentStateRepository = messageEnvironmentStateRepository;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(CreateMessageUseCase.class);
    }

    @Override
    public void createMessage(CreateMessageDTO dto, MessageAccessContext context) {
        var applicationId = environmentResolver.resolveApplication(context);
        validator.validate(dto, applicationId);
        var environmentIds = dto.getEnvironmentIds().stream()
                .map(environmentId -> getUUIDFromString(environmentId).toString())
                .toList();
        var messageEnvironmentStateId = activeMessageEnvironmentStateId();
        var normalizedCode = normalize(dto.getCode());

        try {
            var messageData = new MessageData(
                    UtilUUID.getNewUUID(),
                    normalizedCode,
                    dto.getTitle(),
                    dto.getContent(),
                    new MessageTypeData(UtilUUID.getStringToUUID(dto.getTypeId()), ""),
                    new MessageCategoryData(UtilUUID.getStringToUUID(dto.getCategoryId()), ""),
                    EMPTY,
                    new FunctionalityData(
                            UtilUUID.getStringToUUID(dto.getFunctionalityId()),
                            "",
                            ApplicationData.build(UtilUUID.getStringToUUID(applicationId), EMPTY)
                    )
            );
            messageData.setStatus(new StatusMessageData(UtilUUID.getStringToUUID(dto.getStatusId()), ""));

            createMessageRepository.createMessage(
                    messageData,
                    environmentIds,
                    messageEnvironmentStateId
            );

            log.info("Message created successfully with code: {}", normalizedCode);
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating message in repository", ex);
            throw CrossWordsException.build("Error al crear el mensaje", ex);
        }
    }

    private String activeMessageEnvironmentStateId() {
        var states = messageEnvironmentStateRepository.findAll();
        return (states == null ? List.<MessageEnvironmentStateData>of() : states)
                .stream()
                .filter(state -> state != null && STATE_ACTIVE.equalsIgnoreCase(state.getName()))
                .map(state -> getStringFromUUID(state.getId()))
                .findFirst()
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_197.getCode())));
    }
}
