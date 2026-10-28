package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportDuplicateCodeRuleTest {
    @Mock private CatalogPort catalog;

    @Test
    void validate_acceptsUniqueOrUnobservedNormalizedCodes() {
        var rule = new ImportDuplicateCodeRule(catalog, Map.of("MSG_OK", 1L));

        assertDoesNotThrow(() -> rule.validate("MSG_OK"));
        assertDoesNotThrow(() -> rule.validate("MSG_NEW"));
    }

    @Test
    void validate_rejectsDuplicateNormalizedCode() {
        when(catalog.getMessage("IMP_006")).thenReturn("Código repetido en el archivo.");

        assertThatThrownBy(() -> new ImportDuplicateCodeRule(catalog, Map.of("MSG_DUP", 2L)).validate("MSG_DUP"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        exception -> assertThat(exception.getUserMessage()).contains("Código repetido"));
    }
}
