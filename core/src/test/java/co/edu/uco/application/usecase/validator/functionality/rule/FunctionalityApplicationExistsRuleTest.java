package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunctionalityApplicationExistsRuleTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;

    private FunctionalityApplicationExistsRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new FunctionalityApplicationExistsRule(catalogPort, applicationRepository);
    }

    @Test
    void validate_doesNotThrow_whenApplicationExists() {
        when(applicationRepository.existsById(anyString())).thenReturn(true);
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().applicationId("app-1").build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenApplicationDoesNotExist() {
        when(applicationRepository.existsById(anyString())).thenReturn(false);
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder().applicationId("unknown").build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));
    }
}