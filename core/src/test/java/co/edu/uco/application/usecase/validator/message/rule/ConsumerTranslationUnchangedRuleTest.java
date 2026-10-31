package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.common.catalog.strategy.ConsumerTranslationSnapshot;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationComparison;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerTranslationUnchangedRuleTest {
    @Mock private CatalogPort catalog;

    @Test
    void validate_acceptsEqualSnapshots() {
        var snapshot = snapshot("same", "en");
        var rule = new ConsumerTranslationUnchangedRule(catalog);

        assertThatCode(() -> rule.validate(new ConsumerTranslationComparison(snapshot, snapshot)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_throwsConflictWhenPayloadOrSourceLanguageChanged() {
        when(catalog.getMessage("FUN_220")).thenReturn("message changed");
        var comparison = new ConsumerTranslationComparison(snapshot("before", "en"), snapshot("after", "fr"));
        var rule = new ConsumerTranslationUnchangedRule(catalog);

        assertThatThrownBy(() -> rule.validate(comparison))
                .isInstanceOf(ConflictException.class);
    }

    private static ConsumerTranslationSnapshot snapshot(String title, String source) {
        return new ConsumerTranslationSnapshot(new ResolvedMessageDTO("MSG", title, "body"), source);
    }
}
