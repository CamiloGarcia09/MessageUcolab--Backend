package co.edu.uco.application.usecase.validator;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeValidatorTest {

    @Mock
    private CatalogPort catalogPort;

    @Mock
    private Validator<String> firstValidator;

    @Mock
    private Validator<String> secondValidator;

    @Test
    void validate_delegatesToAllValidators() {
        CompositeValidator<String> composite = new CompositeValidator<>(List.of(firstValidator, secondValidator), catalogPort);

        composite.validate("data");

        verify(firstValidator).validate("data");
        verify(secondValidator).validate("data");
    }

    @Test
    void validate_throwsCrossWordsException_whenNoValidatorsConfigured() {
        when(catalogPort.getMessage("FUN_034")).thenReturn("no validators");
        CompositeValidator<String> composite = new CompositeValidator<>(List.of(), catalogPort);

        assertThatThrownBy(() -> composite.validate("data"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("no validators"));
    }

    @Test
    void validate_throwsCrossWordsException_whenDataIsNull() {
        when(catalogPort.getMessage("FUN_010")).thenReturn("data is null");
        CompositeValidator<String> composite = new CompositeValidator<>(List.of(firstValidator), catalogPort);

        assertThatThrownBy(() -> composite.validate(null))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("data is null"));
        verifyNoInteractions(firstValidator);
    }
}