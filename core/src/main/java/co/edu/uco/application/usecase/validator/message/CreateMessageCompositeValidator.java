package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.Validator;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.message.rule.MessageCatalogIdExistsRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCatalogIdRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCatalogIdUuidRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCodeDuplicatedRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCodeFormatRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCodeMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageCodeRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageContentMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageContentMinLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageContentRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityBelongsApplicationRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityIdRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityIdUuidRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleMinLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleRequiredRule;

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

public final class CreateMessageCompositeValidator extends CompositeValidator<CreateMessageValidationContext> {

    public CreateMessageCompositeValidator(CatalogPort catalogPort,
                                           RecordExistsCatalogPort recordExistsCatalogPort,
                                           FunctionalityCatalogRepository functionalityCatalogRepository,
                                           MessageCodeQueryPort messageCodeQueryPort,
                                           MessageEnvironmentSelectionCompositeValidator environmentSelectionValidator) {
        super(List.of(
                forDto(new MessageCodeRequiredRule(catalogPort)),
                forDto(new MessageCodeMaxLengthRule(catalogPort)),
                forDto(new MessageCodeFormatRule(catalogPort)),
                forDto(new MessageTitleRequiredRule(catalogPort)),
                forDto(new MessageTitleMinLengthRule(catalogPort)),
                forDto(new MessageTitleMaxLengthRule(catalogPort)),
                forDto(new MessageContentRequiredRule(catalogPort)),
                forDto(new MessageContentMinLengthRule(catalogPort)),
                forDto(new MessageContentMaxLengthRule(catalogPort)),
                forDto(new MessageFunctionalityIdRequiredRule(catalogPort)),
                forDto(new MessageFunctionalityIdUuidRule(catalogPort)),
                forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getTypeId, FUN_190)),
                forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getTypeId)),
                forDto(new MessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        CreateMessageDTO::getTypeId, MESSAGE_TYPE, FUN_191)),
                forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getCategoryId, FUN_192)),
                forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getCategoryId)),
                forDto(new MessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        CreateMessageDTO::getCategoryId, MESSAGE_CATEGORY, FUN_193)),
                forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getStatusId, FUN_194)),
                forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getStatusId)),
                forDto(new MessageCatalogIdExistsRule(catalogPort, recordExistsCatalogPort,
                        CreateMessageDTO::getStatusId, MESSAGE_STATE, FUN_195)),
                forEnvironments(environmentSelectionValidator),
                new MessageFunctionalityBelongsApplicationRule(catalogPort, functionalityCatalogRepository),
                new MessageCodeDuplicatedRule(catalogPort, messageCodeQueryPort)
        ), catalogPort);
    }

    public void validate(CreateMessageDTO dto, String applicationId) {
        super.validate(dto == null ? null : new CreateMessageValidationContext(dto, applicationId));
    }

    private static Validator<CreateMessageValidationContext> forEnvironments(
            MessageEnvironmentSelectionCompositeValidator validator) {
        return context -> validator.validate(context.dto().getEnvironmentIds(), context.applicationId());
    }

    private static Validator<CreateMessageValidationContext> forDto(Validator<CreateMessageDTO> validator) {
        return context -> validator.validate(context.dto());
    }
}
