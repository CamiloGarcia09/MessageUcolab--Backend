package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.common.catalog.strategy.ConsumerTranslationSnapshot;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerTranslationValidatorsTest {
    @Mock private CatalogPort catalog;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void consumerTarget_rejectsMissingLanguage(String language) {
        org.mockito.Mockito.lenient().when(catalog.getMessage("FUN_010")).thenReturn("required");
        org.mockito.Mockito.lenient().when(catalog.getMessage("FUN_044")).thenReturn("required");

        assertThatThrownBy(() -> new ConsumerTranslationLanguageValidator(catalog).validate(language))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"English", "en-US-extra-long-tag-that-exceeds-fifty-characters-abcdefghijklmnop", "a", "es-123456789"})
    void consumerTarget_rejectsNamesOrInvalidTagLengths(String language) {
        when(catalog.getMessage("FUN_045")).thenReturn("invalid tag");

        assertThatThrownBy(() -> new ConsumerTranslationLanguageValidator(catalog).validate(language))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh-Hans", "pt-br", "abc-12345678"})
    void consumerTarget_acceptsLanguageTags(String language) {
        assertThatCode(() -> new ConsumerTranslationLanguageValidator(catalog).validate(language))
                .doesNotThrowAnyException();
    }

    @Test
    void applicationBaseLanguage_reportsCataloguedInvalidBaseError() {
        when(catalog.getMessage("FUN_219")).thenReturn("invalid base");

        assertThatThrownBy(() -> new ApplicationBaseLanguageValidator(catalog).validate(" "))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getUserMessage())
                        .isEqualTo("invalid base"));
    }

    @Test
    void consistencyValidator_reportsConflictWhenSnapshotChanges() {
        var before = new ConsumerTranslationComparison(snapshot("old"), snapshot("new"));
        when(catalog.getMessage("FUN_220")).thenReturn("changed during translation");

        assertThatThrownBy(() -> new ConsumerTranslationConsistencyValidator(catalog).validate(before))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(((ConflictException) error).getUserMessage())
                        .isEqualTo("changed during translation"));
    }

    @Test
    void consistencyValidator_acceptsUnchangedSnapshot() {
        var unchanged = snapshot("same");

        assertThatCode(() -> new ConsumerTranslationConsistencyValidator(catalog)
                .validate(new ConsumerTranslationComparison(unchanged, unchanged)))
                .doesNotThrowAnyException();
    }

    private static ConsumerTranslationSnapshot snapshot(String title) {
        return new ConsumerTranslationSnapshot(new ResolvedMessageDTO("MSG", title, "body"), "en");
    }
}
