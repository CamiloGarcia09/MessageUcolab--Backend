package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
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
class FunctionalityApplicationIdUuidRuleTest {

    private static final String APP_UUID = "123e4567-e89b-12d3-a456-426614175000";

    @Mock
    private CatalogPort catalogPort;

    private FunctionalityApplicationIdUuidRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new FunctionalityApplicationIdUuidRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenApplicationIdIsValidUuid() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().applicationId(APP_UUID).build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenApplicationIdIsNotAUuid() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().applicationId("not-a-uuid").build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}