package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.Validator;
import co.edu.uco.application.usecase.domain.message.importing.ImportIssue;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
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
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityActiveRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityIdRequiredRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageFunctionalityIdUuidRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleMaxLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleMinLengthRule;
import co.edu.uco.application.usecase.validator.message.rule.MessageTitleRequiredRule;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

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

    private final List<FieldRule> fieldRules;

    public CreateMessageCompositeValidator(CatalogPort catalogPort,
                                           RecordExistsCatalogPort recordExistsCatalogPort,
                                           FunctionalityCatalogRepository functionalityCatalogRepository,
                                           MessageCodeQueryPort messageCodeQueryPort,
                                           MessageEnvironmentSelectionCompositeValidator environmentSelectionValidator) {
        this(catalogPort, rules(catalogPort, recordExistsCatalogPort, functionalityCatalogRepository,
                messageCodeQueryPort, environmentSelectionValidator));
    }

    private CreateMessageCompositeValidator(CatalogPort catalogPort, List<FieldRule> rules) {
        super(rules.stream().map(FieldRule::validator).toList(), catalogPort);
        this.fieldRules = rules;
    }

    private static List<FieldRule> rules(CatalogPort catalogPort, RecordExistsCatalogPort records,
                                         FunctionalityCatalogRepository functionalities,
                                         MessageCodeQueryPort codes,
                                         MessageEnvironmentSelectionCompositeValidator environments) {
        return List.of(
                new FieldRule("code", forDto(new MessageCodeRequiredRule(catalogPort))),
                new FieldRule("code", forDto(new MessageCodeMaxLengthRule(catalogPort))),
                new FieldRule("code", forDto(new MessageCodeFormatRule(catalogPort))),
                new FieldRule("title", forDto(new MessageTitleRequiredRule(catalogPort))),
                new FieldRule("title", forDto(new MessageTitleMinLengthRule(catalogPort))),
                new FieldRule("title", forDto(new MessageTitleMaxLengthRule(catalogPort))),
                new FieldRule("content", forDto(new MessageContentRequiredRule(catalogPort))),
                new FieldRule("content", forDto(new MessageContentMinLengthRule(catalogPort))),
                new FieldRule("content", forDto(new MessageContentMaxLengthRule(catalogPort))),
                new FieldRule("functionality", forDto(new MessageFunctionalityIdRequiredRule(catalogPort))),
                new FieldRule("functionality", forDto(new MessageFunctionalityIdUuidRule(catalogPort))),
                new FieldRule("type", forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getTypeId, FUN_190))),
                new FieldRule("type", forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getTypeId))),
                new FieldRule("type", forDto(new MessageCatalogIdExistsRule(catalogPort, records,
                        CreateMessageDTO::getTypeId, MESSAGE_TYPE, FUN_191))),
                new FieldRule("category", forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getCategoryId, FUN_192))),
                new FieldRule("category", forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getCategoryId))),
                new FieldRule("category", forDto(new MessageCatalogIdExistsRule(catalogPort, records,
                        CreateMessageDTO::getCategoryId, MESSAGE_CATEGORY, FUN_193))),
                new FieldRule("status", forDto(new MessageCatalogIdRequiredRule(catalogPort, CreateMessageDTO::getStatusId, FUN_194))),
                new FieldRule("status", forDto(new MessageCatalogIdUuidRule(catalogPort, CreateMessageDTO::getStatusId))),
                new FieldRule("status", forDto(new MessageCatalogIdExistsRule(catalogPort, records,
                        CreateMessageDTO::getStatusId, MESSAGE_STATE, FUN_195))),
                new FieldRule("environments", forEnvironments(environments)),
                new FieldRule("functionality", new MessageFunctionalityBelongsApplicationRule(catalogPort, functionalities)),
                new FieldRule("functionality", new MessageFunctionalityActiveRule(catalogPort, functionalities)),
                new FieldRule("code", new MessageCodeDuplicatedRule(catalogPort, codes))
        );
    }

    /** One actionable error per field; dependent rules are skipped after a field fails. */
    public List<ImportIssue> collectErrors(CreateMessageDTO dto, String applicationId) {
        return collectErrors(dto, applicationId, java.util.Set.of());
    }

    /** Batch environments must already have passed the shared environment composite once. */
    public List<ImportIssue> collectRowErrors(CreateMessageDTO dto, String applicationId) {
        return collectErrors(dto, applicationId, java.util.Set.of("environments"));
    }

    private List<ImportIssue> collectErrors(CreateMessageDTO dto, String applicationId, java.util.Set<String> validatedFields) {
        var context = new CreateMessageValidationContext(dto, applicationId);
        var failedFields = new HashSet<>(validatedFields);
        var issues = new ArrayList<ImportIssue>();
        for (var rule : fieldRules) {
            if (!failedFields.contains(rule.field())) {
                collect(rule, context, failedFields, issues);
            }
        }
        return List.copyOf(issues);
    }

    private static void collect(FieldRule rule, CreateMessageValidationContext context,
                                java.util.Set<String> failedFields, List<ImportIssue> issues) {
        try {
            rule.validator().validate(context);
        } catch (CrossWordsException ex) {
            if (ex.getHttpStatus() >= 500) {
                throw ex;
            }
            failedFields.add(rule.field());
            issues.add(new ImportIssue(rule.field(), ex.getUserMessage()));
        }
    }

    private record FieldRule(String field, Validator<CreateMessageValidationContext> validator) { }

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
