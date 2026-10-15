package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UpdateMessageTitleMaxLengthRuleTest {

    @Mock
    private CatalogPort catalogPort;

    private UpdateMessageTitleMaxLengthRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new UpdateMessageTitleMaxLengthRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenTitleDoesNotExceedMaximum() {
        UpdateMessageDTO dto = UpdateMessageDTO.builder().title("A valid title").build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenTitleExceedsMaximum() {
        UpdateMessageDTO dto = UpdateMessageDTO.builder().title("a".repeat(51)).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

    @Test
    void validate_doesNotThrow_whenTitleIsEmpty() {
        UpdateMessageDTO dto = new UpdateMessageDTO();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}
