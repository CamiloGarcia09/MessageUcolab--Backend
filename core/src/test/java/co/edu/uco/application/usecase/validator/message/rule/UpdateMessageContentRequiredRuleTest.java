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
class UpdateMessageContentRequiredRuleTest {

    @Mock
    private CatalogPort catalogPort;

    private UpdateMessageContentRequiredRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new UpdateMessageContentRequiredRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenContentIsPresent() {
        UpdateMessageDTO dto = UpdateMessageDTO.builder().content("Valid content text").build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenContentIsEmpty() {
        UpdateMessageDTO dto = new UpdateMessageDTO();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}
