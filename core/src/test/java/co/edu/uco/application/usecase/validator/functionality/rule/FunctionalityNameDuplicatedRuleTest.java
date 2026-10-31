package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
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
class FunctionalityNameDuplicatedRuleTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private FunctionalityRepository functionalityRepository;

    private FunctionalityNameDuplicatedRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new FunctionalityNameDuplicatedRule(catalogPort, functionalityRepository);
    }

    @Test
    void validate_doesNotThrow_whenNameIsNotRegisteredForApplication() {
        when(functionalityRepository.existsByNameAndApplicationId(anyString(), anyString())).thenReturn(false);
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder()
                .name("Send message")
                .applicationId("app-1")
                .build();

        assertDoesNotThrow(() -> rule.validate(dto));
    }

    @Test
    void validate_throwsBusinessRuleException_whenNameAlreadyExistsForApplication() {
        when(functionalityRepository.existsByNameAndApplicationId(anyString(), anyString())).thenReturn(true);
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder()
                .name("Send message")
                .applicationId("app-1")
                .build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }
}