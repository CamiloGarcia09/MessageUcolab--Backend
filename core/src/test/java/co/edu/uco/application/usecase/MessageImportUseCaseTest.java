package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.*;
import co.edu.uco.application.secondaryports.repository.*;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.validator.message.importing.ImportConfirmationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportUseCaseTest {
    private static final String APP = "123e4567-e89b-12d3-a456-426614175106";
    private static final String ENV = "123e4567-e89b-12d3-a456-426614175107";
    private static final String FUNCTION = "123e4567-e89b-12d3-a456-426614175108";
    private static final String TYPE = "123e4567-e89b-12d3-a456-426614175109";
    private static final String CATEGORY = "123e4567-e89b-12d3-a456-426614175110";
    private static final String STATE = "123e4567-e89b-12d3-a456-426614175111";
    private static final String ENV_STATE = "123e4567-e89b-12d3-a456-426614175112";
    private static final MessageAccessContext CONTEXT = new MessageAccessContext(APP);
    @Mock private CatalogPort catalog;
    @Mock private MessageEnvironmentResolver access;
    @Mock private MessageEnvironmentSelectionCompositeValidator environments;
    @Mock private FunctionalityCatalogRepository functionalities;
    @Mock private MessageTypeCatalogRepository types;
    @Mock private MessageCategoryCatalogRepository categories;
    @Mock private MessageStateCatalogRepository states;
    @Mock private MessageEnvironmentStateCatalogRepository environmentStates;
    @Mock private MessageCodeQueryPort codes;
    @Mock private MessageImportDraftStore drafts;
    @Mock private MessageImportRepository repository;
    @Mock private ImportConfirmationCompositeValidator confirmation;
    private MessageImportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MessageImportUseCase(catalog, access, environments, functionalities, types, categories,
                states, environmentStates, codes, drafts, repository, confirmation);
        lenient().when(access.resolveApplication(CONTEXT)).thenReturn(APP);
        lenient().when(functionalities.findAllByApplicationId(APP)).thenReturn(functionalityCatalog());
        lenient().when(types.findAll()).thenReturn(List.of(new MessageTypeData(UUID.fromString(TYPE), "Type")));
        lenient().when(categories.findAll()).thenReturn(List.of(new MessageCategoryData(UUID.fromString(CATEGORY), "Category")));
        lenient().when(states.findAll()).thenReturn(List.of(new StatusMessageData(UUID.fromString(STATE), "Active")));
        lenient().when(codes.findExistingCodes(anySet(), eq(APP))).thenReturn(Set.of());
    }

    @Test
    void references_listsOnlyOwnedFunctionalitiesAndMarksInactiveOnesUnavailable() {
        var inactive = functionality("Inactive", false);
        when(functionalities.findAllByApplicationId(APP)).thenReturn(List.of(functionalityCatalog().getFirst(), inactive));

        var result = useCase.references(CONTEXT);

        assertThat(result.functionalities()).extracting(ImportReference::available).containsExactly(true, false);
        verify(functionalities).findAllByApplicationId(APP);
    }

    @Test
    void validate_normalizesEnvironmentAndCodesQueriesBulkThenStoresPreview() {
        var upperEnvironment = ENV.toUpperCase();
        var rows = List.of(row(2, " greet "));
        when(codes.findExistingCodes(Set.of("MSG_GREET"), APP)).thenReturn(Set.of());
        var preview = useCase.validate(rows, List.of(upperEnvironment), CONTEXT);

        assertThat(preview.environmentIds()).containsExactly(ENV);
        assertThat(preview.rows()).singleElement().satisfies(result -> {
            assertThat(result.normalizedCode()).isEqualTo("MSG_GREET");
            assertThat(result.issues()).isEmpty();
        });
        assertThat(java.time.Duration.between(Instant.now(), preview.expiresAt())).isBetween(
                MessageImportUseCase.PREVIEW_TTL.minusSeconds(2), MessageImportUseCase.PREVIEW_TTL);
        verify(environments).validate(List.of(upperEnvironment), APP);
        verify(codes).findExistingCodes(Set.of("MSG_GREET"), APP);
        verify(drafts).save(preview);
    }

    @Test
    void validate_reportsAmbiguousOrUnknownReferenceWithoutLosingOtherRows() {
        var ambiguous = new ImportRow(2, "FIRST", "Duplicate", TYPE, CATEGORY, STATE, "A valid title", "A valid content");
        var secondFunction = "123e4567-e89b-12d3-a456-426614175113";
        var good = new ImportRow(3, "SECOND", "Unique", "Type", "Category", "Active", "A valid title", "A valid content");
        when(functionalities.findAllByApplicationId(APP)).thenReturn(List.of(
                functionality("Duplicate", FUNCTION, true), functionality("Duplicate", secondFunction, true),
                functionality("Unique", secondFunction, true)));

        var preview = useCase.validate(List.of(ambiguous, good), List.of(ENV), CONTEXT);

        assertThat(preview.rows()).hasSize(2);
        assertThat(preview.rows().getFirst().issues()).extracting(ImportIssue::field).contains("functionality");
        assertThat(preview.rows().getFirst().message().getFunctionalityId()).isEmpty();
        assertThat(preview.rows().get(1).issues()).isEmpty();
    }

    @Test
    void validate_propagatesTechnicalLookupFailureInsteadOfConvertingItToRowIssue() {
        var failure = BusinessRuleException.buildTechnicalException("catalog lookup failed");
        when(functionalities.findAllByApplicationId(APP)).thenReturn(List.of());
        // A technical failure from code lookup must abort validation, not produce an import preview.
        when(codes.findExistingCodes(anySet(), eq(APP))).thenThrow(failure);

        assertThatThrownBy(() -> useCase.validate(List.of(row(2, "BROKEN")), List.of(ENV), CONTEXT))
                .isSameAs(failure);
        verify(drafts, never()).save(any(MessageImportPreview.class));
    }

    @Test
    void confirm_returnsDurableReceiptBeforeReadingExpiredDraft() {
        var receipt = receipt("import-1");
        when(repository.findReceipt("import-1", APP)).thenReturn(Optional.of(receipt));

        var result = useCase.confirm("import-1", false, CONTEXT);

        assertThat(result).isSameAs(receipt);
        verifyNoInteractions(drafts, confirmation, environmentStates);
    }

    @Test
    void confirm_revalidatesAndPersistsCurrentPreviewBeforeAtomicCreate() {
        var original = preview("import-2", row(2, "NEW_CODE"), Instant.now().plusSeconds(500));
        var receipt = receipt("import-2");
        when(repository.findReceipt("import-2", APP)).thenReturn(Optional.empty());
        when(drafts.find("import-2", APP)).thenReturn(Optional.of(original));
        when(environmentStates.findAll()).thenReturn(List.of(new MessageEnvironmentStateData(UUID.fromString(ENV_STATE), "Active")));
        when(repository.createAtomically(any(MessageImportPreview.class), eq(ENV_STATE))).thenReturn(receipt);

        var result = useCase.confirm("import-2", false, CONTEXT);

        assertThat(result).isSameAs(receipt);
        verify(drafts).save(any(MessageImportPreview.class));
        verify(repository).createAtomically(any(MessageImportPreview.class), eq(ENV_STATE));
    }

    @Test
    void confirm_preservesResolvedIdentityWhenOldNameIsReusedByAnotherFunctionality() {
        var originalFunction = functionality("Function", FUNCTION, true);
        when(functionalities.findAllByApplicationId(APP)).thenReturn(List.of(originalFunction), List.of(
                functionality("Renamed Function", FUNCTION, true),
                functionality("Function", "123e4567-e89b-12d3-a456-426614175113", true)));
        var validated = useCase.validate(List.of(row(2, "STABLE")), List.of(ENV), CONTEXT);
        when(repository.findReceipt("stable-import", APP)).thenReturn(Optional.empty());
        when(drafts.find("stable-import", APP)).thenReturn(Optional.of(new MessageImportPreview("stable-import", APP,
                validated.environmentIds(), validated.rows(), Instant.now().plusSeconds(500))));
        when(environmentStates.findAll()).thenReturn(List.of(
                new MessageEnvironmentStateData(UUID.fromString(ENV_STATE), "Active")));
        when(repository.createAtomically(any(MessageImportPreview.class), eq(ENV_STATE)))
                .thenReturn(receipt("stable-import"));
        useCase = useCaseWithRealConfirmation();

        useCase.confirm("stable-import", false, CONTEXT);

        var captor = org.mockito.ArgumentCaptor.forClass(MessageImportPreview.class);
        verify(repository).createAtomically(captor.capture(), eq(ENV_STATE));
        assertThat(captor.getValue().rows().getFirst().message().getFunctionalityId()).isEqualTo(FUNCTION);
    }

    @Test
    void confirm_blocksImportWhenResolvedFunctionalityBecomesInactive() {
        when(functionalities.findAllByApplicationId(APP)).thenReturn(List.of(functionality("Function", FUNCTION, true)),
                List.of(functionality("Function", FUNCTION, false)));
        var validated = useCase.validate(List.of(row(2, "INACTIVE")), List.of(ENV), CONTEXT);
        when(repository.findReceipt("inactive-import", APP)).thenReturn(Optional.empty());
        when(drafts.find("inactive-import", APP)).thenReturn(Optional.of(new MessageImportPreview("inactive-import", APP,
                validated.environmentIds(), validated.rows(), Instant.now().plusSeconds(500))));
        when(catalog.getMessage("IMP_008")).thenReturn("Rows invalid");
        useCase = useCaseWithRealConfirmation();

        assertThatThrownBy(() -> useCase.confirm("inactive-import", false, CONTEXT))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).createAtomically(any(MessageImportPreview.class), any());
    }

    @Test
    void confirm_keepsUpdatedInvalidPreviewAndSkipsPersistenceWhenCodeBecameExisting() {
        var original = preview("import-3", row(2, "TAKEN"), Instant.now().plusSeconds(500));
        when(repository.findReceipt("import-3", APP)).thenReturn(Optional.empty());
        when(drafts.find("import-3", APP)).thenReturn(Optional.of(original));
        when(codes.findExistingCodes(Set.of("MSG_TAKEN"), APP)).thenReturn(Set.of("MSG_TAKEN"));

        assertThatThrownBy(() -> useCase.confirm("import-3", false, CONTEXT))
                .isInstanceOf(BusinessRuleException.class);

        var captor = org.mockito.ArgumentCaptor.forClass(MessageImportPreview.class);
        verify(drafts).save(captor.capture());
        assertThat(captor.getValue().rows().getFirst().issues()).extracting(ImportIssue::field).contains("code");
        verify(repository, never()).createAtomically(any(MessageImportPreview.class), any());
    }

    @Test
    void confirm_returnsReceiptCommittedByConcurrentRequestAfterValidationFailure() {
        var expired = preview("import-4", row(2, "CODE"), Instant.now().minusSeconds(1));
        var receipt = receipt("import-4");
        when(repository.findReceipt("import-4", APP)).thenReturn(Optional.empty(), Optional.of(receipt));
        when(drafts.find("import-4", APP)).thenReturn(Optional.of(expired));
        when(catalog.getMessage("IMP_007")).thenReturn("Preview expired");

        assertThat(useCase.confirm("import-4", false, CONTEXT)).isSameAs(receipt);
        verify(drafts, never()).save(any(MessageImportPreview.class));
    }

    @Test
    void confirm_propagatesOriginalDomainFailureWhenNoConcurrentReceiptAppears() {
        var expired = preview("import-5", row(2, "CODE"), Instant.now().minusSeconds(1));
        var failure = BusinessRuleException.buildUserException("Preview expired");
        when(repository.findReceipt("import-5", APP)).thenReturn(Optional.empty());
        when(drafts.find("import-5", APP)).thenReturn(Optional.of(expired));
        when(catalog.getMessage("IMP_007")).thenReturn("Preview expired");

        assertThatThrownBy(() -> useCase.confirm("import-5", false, CONTEXT))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, times(2)).findReceipt("import-5", APP);
    }

    private static List<FunctionalityData> functionalityCatalog() {
        return List.of(functionality("Function", true));
    }

    private static FunctionalityData functionality(String name, boolean active) {
        return functionality(name, FUNCTION, active);
    }

    private static FunctionalityData functionality(String name, String id, boolean active) {
        var item = new FunctionalityData(UUID.fromString(id), name,
                ApplicationData.build(UUID.fromString(APP), "Application"));
        item.setStateId(UUID.fromString(STATE));
        item.setState(active ? "Active" : "Inactive");
        return item;
    }

    private static ImportRow row(int number, String code) {
        return new ImportRow(number, code, "Function", "Type", "Category", "Active", "A valid title", "A valid content");
    }

    private static MessageImportPreview preview(String id, ImportRow row, Instant expiry) {
        var dto = CreateMessageDTO.builder().code(row.code()).title(row.title()).content(row.content())
                .functionalityId(FUNCTION).typeId(TYPE).categoryId(CATEGORY).statusId(STATE).environmentIds(List.of(ENV)).build();
        return new MessageImportPreview(id, APP, List.of(ENV),
                List.of(new ImportRowResult(row, "MSG_" + row.code(), dto, List.of())), expiry);
    }

    private static MessageImportReceipt receipt(String id) {
        return new MessageImportReceipt(id, 1, List.of(ENV), Instant.now());
    }

    private MessageImportUseCase useCaseWithRealConfirmation() {
        return new MessageImportUseCase(catalog, access, environments, functionalities, types, categories, states,
                environmentStates, codes, drafts, repository,
                new ImportConfirmationCompositeValidator(catalog, org.mockito.Mockito.mock(EnvironmentRepository.class)));
    }
}
