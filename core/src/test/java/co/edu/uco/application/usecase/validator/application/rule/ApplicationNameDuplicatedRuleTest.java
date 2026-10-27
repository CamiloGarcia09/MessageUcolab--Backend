package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationNameDuplicatedRuleTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;

    private ApplicationNameDuplicatedRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new ApplicationNameDuplicatedRule(catalogPort, applicationRepository);
    }

    @Test
    void validate_doesNotThrow_whenNameIsNotRegistered() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().name("Message App").build();

        when(applicationRepository.existsByNameAndOrganization("Message App", null)).thenReturn(false);
        assertDoesNotThrow(() -> rule.validate(dto));
        verify(applicationRepository).existsByNameAndOrganization("Message App", null);
    }

    @Test
    void validate_throwsBusinessRuleException_whenNameAlreadyExists() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().name("Message App").build();
        when(applicationRepository.existsByNameAndOrganization("Message App", null)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));
    }

}
