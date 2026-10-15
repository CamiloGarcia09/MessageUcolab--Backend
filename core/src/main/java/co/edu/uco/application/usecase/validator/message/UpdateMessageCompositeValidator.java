package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.Validator;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageCatalogIdExistsRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageCatalogIdRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageCatalogIdUuidRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageContentMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageContentMinLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageContentRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageFunctionalityBelongsApplicationRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageFunctionalityIdRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageFunctionalityIdUuidRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageTitleMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageTitleMinLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.UpdateMessageTitleRequiredRule;

import java.util.List;

import static co.edu.uco.application.secondaryports.repository.ReferenceCatalog.MESSAGE_CATEGORY;
import static co.edu.uco.application.secondaryports.repository.ReferenceCatalog.MESSAGE_STATE;
import static co.edu.uco.application.secondaryports.repository.ReferenceCatalog.MESSAGE_TYPE;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_190;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_191;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_192;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_193;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_194;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_195;

public final class UpdateMessageCompositeValidator extends CompositeValidator<UpdateMessageValidationContext> {

    public UpdateMessageCompositeValidator(CatalogPort catalogPort,
                                           RecordExistsCatalogPort recordExistsCatalogPort,
                                           FunctionalityCatalogRepository functionalityCatalogRepository) {
        super(List.of(
                forDto(new UpdateMessageTitleRequiredRule(catalogPort)),
                forDto(new UpdateMessageTitleMinLengthRule(catalogPort)),
                forDto(new UpdateMessageTitleMaxLengthRule(catalogPort)),
                forDto(new UpdateMessageContentRequiredRule(catalogPort)),
                forDto(new UpdateMessageContentMinLengthRule(catalogPort)),
                forDto(new UpdateMessageContentMaxLengthRule(catalogPort)),
                forDto(new UpdateMessageFunctionalityIdRequiredRule(catalogPort)),
                forDto(new UpdateMessageFunctionalityIdUuidRule(catalogPort)),
                forDto(new UpdateMessageCatalogIdRequiredRule(catalogPort, UpdateMessageDTO::getTypeId, FUN_190)),
                forDto(new UpdateMessageCatalogIdUuidRule(catalogPort, UpdateMessageDTO::getTypeId)),
                forDto(new UpdateMessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        UpdateMessageDTO::getTypeId, MESSAGE_TYPE, FUN_191)),
                forDto(new UpdateMessageCatalogIdRequiredRule(catalogPort, UpdateMessageDTO::getCategoryId, FUN_192)),
                forDto(new UpdateMessageCatalogIdUuidRule(catalogPort, UpdateMessageDTO::getCategoryId)),
                forDto(new UpdateMessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        UpdateMessageDTO::getCategoryId, MESSAGE_CATEGORY, FUN_193)),
                forDto(new UpdateMessageCatalogIdRequiredRule(catalogPort, UpdateMessageDTO::getStatusId, FUN_194)),
                forDto(new UpdateMessageCatalogIdUuidRule(catalogPort, UpdateMessageDTO::getStatusId)),
                forDto(new UpdateMessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        UpdateMessageDTO::getStatusId, MESSAGE_STATE, FUN_195)),
                new UpdateMessageFunctionalityBelongsApplicationRule(catalogPort, functionalityCatalogRepository)
        ), catalogPort);
    }

    public void validate(UpdateMessageDTO dto, String applicationId) {
        super.validate(dto == null ? null : new UpdateMessageValidationContext(dto, applicationId));
    }

    private static Validator<UpdateMessageValidationContext> forDto(Validator<UpdateMessageDTO> validator) {
        return context -> validator.validate(context.dto());
    }
}
