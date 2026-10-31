package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.ImportReference;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReferenceRuleTest {
    @Mock private CatalogPort catalog;
    private final List<ImportReference> references = List.of(new ImportReference("id-1", "Payments", true));

    @Test
    void validate_acceptsExactNameAndBlankOptionalReference() {
        var rule = new ImportReferenceRule(catalog, references);

        assertDoesNotThrow(() -> rule.validate("Payments"));
        assertDoesNotThrow(() -> rule.validate(" "));
    }

    @Test
    void validate_rejectsUnknownUuidAndLegacyLabel() {
        when(catalog.getMessage("IMP_005")).thenReturn("Referencia no válida.");

        var rule = new ImportReferenceRule(catalog, references);
        for (var invalid : List.of("Unknown", "id-1", "123e4567-e89b-12d3-a456-426614174000",
                "Payments [id-1]")) {
            assertThatThrownBy(() -> rule.validate(invalid))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            exception -> assertThat(exception.getUserMessage()).contains("Referencia no válida"));
        }
    }

    @Test
    void validate_rejectsAmbiguousName() {
        when(catalog.getMessage("IMP_005")).thenReturn("Referencia no válida.");
        var ambiguous = List.of(new ImportReference("id-1", "Payments", true),
                new ImportReference("id-2", "Payments", true));

        assertThatThrownBy(() -> new ImportReferenceRule(catalog, ambiguous).validate("Payments"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
