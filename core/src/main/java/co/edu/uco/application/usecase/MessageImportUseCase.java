package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.*;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.message.importing.*;
import co.edu.uco.application.usecase.validator.specification.impl.ActiveFunctionalitySpecification;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.*;
import static co.edu.uco.crosscutting.helpers.UtilMessageCode.normalize;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isActive;

/** Framework-free orchestration. Excel, Redis and persistence live behind adapters. */
public final class MessageImportUseCase {
    public static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private final CatalogPort catalog;
    private final MessageEnvironmentResolver access;
    private final MessageEnvironmentSelectionCompositeValidator environments;
    private final FunctionalityCatalogRepository functionalities;
    private final MessageTypeCatalogRepository types;
    private final MessageCategoryCatalogRepository categories;
    private final MessageStateCatalogRepository states;
    private final MessageEnvironmentStateCatalogRepository environmentStates;
    private final MessageCodeQueryPort codes;
    private final MessageImportDraftStore drafts;
    private final MessageImportRepository repository;
    private final ImportConfirmationCompositeValidator confirmation;

    public MessageImportUseCase(CatalogPort catalog, MessageEnvironmentResolver access,
            MessageEnvironmentSelectionCompositeValidator environments, FunctionalityCatalogRepository functionalities,
            MessageTypeCatalogRepository types, MessageCategoryCatalogRepository categories,
            MessageStateCatalogRepository states, MessageEnvironmentStateCatalogRepository environmentStates,
            MessageCodeQueryPort codes, MessageImportDraftStore drafts, MessageImportRepository repository,
            ImportConfirmationCompositeValidator confirmation) {
        this.catalog = catalog;
        this.access = access;
        this.environments = environments;
        this.functionalities = functionalities;
        this.types = types;
        this.categories = categories;
        this.states = states;
        this.environmentStates = environmentStates;
        this.codes = codes;
        this.drafts = drafts;
        this.repository = repository;
        this.confirmation = confirmation;
    }

    public ImportReferences references(MessageAccessContext context) {
        return loadReferences(access.resolveApplication(context));
    }

    private ImportReferences loadReferences(String applicationId) {
        var active = new ActiveFunctionalitySpecification();
        return new ImportReferences(functionalities.findAllByApplicationId(applicationId).stream()
                .map(item -> new ImportReference(item.getId().toString(), item.getName(), active.isSatisfiedBy(item))).toList(),
                types.findAll().stream().map(item -> new ImportReference(item.getId().toString(), item.getName(), true)).toList(),
                categories.findAll().stream().map(item -> new ImportReference(item.getId().toString(), item.getName(), true)).toList(),
                states.findAll().stream().map(item -> new ImportReference(item.getId().toString(), item.getName(), true)).toList());
    }

    public MessageImportPreview validate(List<ImportRow> rows, List<String> environmentIds, MessageAccessContext context) {
        var applicationId = access.resolveApplication(context);
        environments.validate(environmentIds, applicationId);
        var canonicalEnvironments = environmentIds.stream().map(id -> UUID.fromString(id).toString()).toList();
        new ImportRowsRule(catalog).validate(rows);
        var results = validateRows(rows, canonicalEnvironments, applicationId);
        var preview = new MessageImportPreview(UUID.randomUUID().toString(), applicationId, canonicalEnvironments,
                results, Instant.now().plus(PREVIEW_TTL));
        drafts.save(preview);
        return preview;
    }

    public MessageImportPreview preview(String importId, MessageAccessContext context) {
        var applicationId = access.resolveApplication(context);
        return drafts.find(importId, applicationId)
                .filter(value -> value.expiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalog.getMessage(IMP_007.getCode())));
    }

    public MessageImportReceipt confirm(String importId, boolean productionConfirmed, MessageAccessContext context) {
        var applicationId = access.resolveApplication(context);
        var receipt = repository.findReceipt(importId, applicationId);
        if (receipt.isPresent()) {
            return receipt.get();
        }
        try {
            return confirmNew(importId, productionConfirmed, context, applicationId);
        } catch (CrossWordsException ex) {
            // Another request may have committed this exact draft during revalidation.
            var completed = repository.findReceipt(importId, applicationId);
            if (completed.isPresent()) {
                return completed.get();
            }
            throw ex;
        }
    }

    public Optional<MessageImportReceipt> result(String importId, MessageAccessContext context) {
        return repository.findReceipt(importId, access.resolveApplication(context));
    }

    private MessageImportReceipt confirmNew(String importId, boolean productionConfirmed,
                                              MessageAccessContext context, String applicationId) {
        var preview = preview(importId, context);
        environments.validate(preview.environmentIds(), applicationId);
        confirmation.validate(new ImportConfirmationCompositeValidator.Context(preview, productionConfirmed));
        var current = new MessageImportPreview(importId, applicationId, preview.environmentIds(),
                revalidateRows(preview), preview.expiresAt());
        // Keep the actionable report if catalogs or message codes changed after the preview.
        drafts.save(current);
        confirmation.validate(new ImportConfirmationCompositeValidator.Context(current, productionConfirmed));
        var activeState = environmentStates.findAll().stream()
                .filter(state -> isActive(state.getName()))
                .findFirst().orElseThrow(() -> BusinessRuleException.buildUserException(catalog.getMessage(FUN_197.getCode())));
        return repository.createAtomically(current, activeState.getId().toString());
    }

    private List<ImportRowResult> revalidateRows(MessageImportPreview preview) {
        // Preserve resolved identities if a catalog name is renamed or reused after validation.
        var rows = preview.rows().stream().map(ImportRowResult::row).toList();
        var validation = rowValidation(rows, preview.applicationId());
        return preview.rows().stream().map(result -> validateMessage(result.row(), result.message(),
                preview.applicationId(), validation.validator(), validation.duplicates(), new ArrayList<>())).toList();
    }

    private List<ImportRowResult> validateRows(List<ImportRow> rows, List<String> environmentIds, String applicationId) {
        var validation = rowValidation(rows, applicationId);
        return rows.stream().map(row -> validateRow(row, environmentIds, applicationId, validation.references(),
                validation.validator(), validation.duplicates())).toList();
    }

    private RowValidation rowValidation(List<ImportRow> rows, String applicationId) {
        var references = loadReferences(applicationId);
        var functionalitySnapshot = functionalities.findAllByApplicationId(applicationId);
        var existing = codes.findExistingCodes(rows.stream().map(row -> normalize(row.code())).collect(Collectors.toSet()), applicationId);
        RecordExistsCatalogPort snapshot = (reference, id) -> switch (reference) {
            case MESSAGE_TYPE -> contains(references.types(), id);
            case MESSAGE_CATEGORY -> contains(references.categories(), id);
            case MESSAGE_STATE -> contains(references.states(), id);
            default -> false;
        };
        var validator = new CreateMessageCompositeValidator(catalog, snapshot, app -> functionalitySnapshot,
                (code, app) -> existing.contains(code), environments);
        var counts = rows.stream().map(row -> normalize(row.code()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        var duplicates = new ImportDuplicateCodeRule(catalog, counts);
        return new RowValidation(references, validator, duplicates);
    }

    private record RowValidation(ImportReferences references, CreateMessageCompositeValidator validator,
                                 ImportDuplicateCodeRule duplicates) { }

    private ImportRowResult validateRow(ImportRow row, List<String> environmentIds, String applicationId,
            ImportReferences refs, CreateMessageCompositeValidator validator, ImportDuplicateCodeRule duplicates) {
        var issues = new ArrayList<ImportIssue>();
        var dto = new CreateMessageDTO();
        dto.setCode(row.code());
        dto.setTitle(row.title());
        dto.setContent(row.content());
        dto.setEnvironmentIds(environmentIds);
        dto.setFunctionalityId(resolve(row.functionality(), "functionality", refs.functionalities(), issues));
        dto.setTypeId(resolve(row.type(), "type", refs.types(), issues));
        dto.setCategoryId(resolve(row.category(), "category", refs.categories(), issues));
        dto.setStatusId(resolve(row.status(), "status", refs.states(), issues));
        return validateMessage(row, dto, applicationId, validator, duplicates, issues);
    }

    private ImportRowResult validateMessage(ImportRow row, CreateMessageDTO dto, String applicationId,
            CreateMessageCompositeValidator validator, ImportDuplicateCodeRule duplicates, List<ImportIssue> issues) {
        var referenceFields = issues.stream().map(ImportIssue::field).collect(Collectors.toSet());
        validator.collectRowErrors(dto, applicationId).stream()
                .filter(issue -> !referenceFields.contains(issue.field())).forEach(issues::add);
        var normalized = normalize(dto.getCode());
        if (issues.stream().noneMatch(issue -> "code".equals(issue.field()))) {
            try {
                duplicates.validate(normalized);
            } catch (BusinessRuleException ex) {
                issues.add(new ImportIssue("code", ex.getUserMessage()));
            }
        }
        return new ImportRowResult(row, normalized, dto, issues);
    }

    private String resolve(String raw, String field, List<ImportReference> references, List<ImportIssue> issues) {
        var value = trim(raw);
        try {
            new ImportReferenceRule(catalog, references).validate(value);
        } catch (BusinessRuleException ex) {
            if (ex.getHttpStatus() >= 500) {
                throw ex;
            }
            issues.add(new ImportIssue(field, ex.getUserMessage()));
            return "";
        }
        return references.stream().filter(ref -> ref.matches(value)).map(ImportReference::id).findFirst().orElse("");
    }

    private static boolean contains(List<ImportReference> references, String id) {
        return references.stream().anyMatch(ref -> ref.id().equals(id));
    }
}
