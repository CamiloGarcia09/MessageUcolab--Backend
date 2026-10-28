package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.ImportRow;
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
class ImportRowsRuleTest {
    @Mock private CatalogPort catalog;

    @Test
    void validate_acceptsRowsAtMaximumAndOriginalSpreadsheetBounds() {
        var rows = java.util.stream.IntStream.rangeClosed(2, 501)
                .mapToObj(number -> row(number)).toList();

        assertDoesNotThrow(() -> new ImportRowsRule(catalog).validate(rows));
    }

    @Test
    void validate_rejectsEmptyInputWithCataloguedDomainError() {
        when(catalog.getMessage("IMP_002")).thenReturn("El archivo debe contener entre 1 y 500 filas.");

        assertThatThrownBy(() -> new ImportRowsRule(catalog).validate(List.of()))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getUserMessage()).contains("500 filas"));
    }

    @Test
    void validate_rejectsDuplicateOriginalRowNumbers() {
        when(catalog.getMessage("IMP_002")).thenReturn("Filas inválidas.");

        assertThatThrownBy(() -> new ImportRowsRule(catalog).validate(List.of(row(2), row(2))))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getUserMessage()).contains("Filas inválidas"));
    }

    private static ImportRow row(int number) {
        return new ImportRow(number, "CODE", "f", "t", "c", "s", "A title", "Some content");
    }
}
