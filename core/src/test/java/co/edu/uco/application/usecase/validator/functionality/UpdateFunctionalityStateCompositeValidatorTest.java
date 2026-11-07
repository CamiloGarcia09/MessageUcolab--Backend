package co.edu.uco.application.usecase.validator.functionality;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFunctionalityStateCompositeValidatorTest {
    private static final String FUNCTIONALITY = "123e4567-e89b-12d3-a456-426614175801";
    private static final String APPLICATION = "123e4567-e89b-12d3-a456-426614175802";
    private static final String STATE = "123e4567-e89b-12d3-a456-426614175803";
    @Mock private CatalogPort catalog;
    @Mock private UpdateFunctionalityStateRepository repository;
    private UpdateFunctionalityStateCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UpdateFunctionalityStateCompositeValidator(catalog, repository);
    }

    @Test
    void validate_acceptsOwnedFunctionalityAndRecognizedActiveState() {
        when(repository.existsInApplication(FUNCTIONALITY, APPLICATION)).thenReturn(true);
        when(repository.findStateNameById(STATE)).thenReturn(Optional.of("Active"));

        assertThatCode(() -> validator.validate(new FunctionalityStateChange(FUNCTIONALITY, APPLICATION, STATE)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsFunctionalityOutsideApplication() {
        when(repository.existsInApplication(FUNCTIONALITY, APPLICATION)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(new FunctionalityStateChange(FUNCTIONALITY, APPLICATION, STATE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validate_rejectsUnknownStateAndMalformedIds() {
        when(repository.existsInApplication(FUNCTIONALITY, APPLICATION)).thenReturn(true);
        when(repository.findStateNameById(STATE)).thenReturn(Optional.of("Retired"));

        assertThatThrownBy(() -> validator.validate(new FunctionalityStateChange(FUNCTIONALITY, APPLICATION, STATE)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> validator.validate(new FunctionalityStateChange("bad", APPLICATION, STATE)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
