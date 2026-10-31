package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationStateExistsRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String MALFORMED_UUID = "not-a-uuid";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;

    private ApplicationStateExistsRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new ApplicationStateExistsRule(catalogPort, recordExistsCatalogPort);
    }

    @Test
    void validate_doesNotThrow_whenStateExists() {
        when(recordExistsCatalogPort.exists(ReferenceCatalog.APPLICATION_STATE, VALID_UUID)).thenReturn(true);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().stateId(VALID_UUID).build();

        assertDoesNotThrow(() -> rule.validate(dto));

        verify(recordExistsCatalogPort).exists(ReferenceCatalog.APPLICATION_STATE, VALID_UUID);
    }

    @Test
    void validate_throwsBusinessRuleException_whenStateDoesNotExist() {
        when(recordExistsCatalogPort.exists(ReferenceCatalog.APPLICATION_STATE, VALID_UUID)).thenReturn(false);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().stateId(VALID_UUID).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));

        verify(recordExistsCatalogPort).exists(ReferenceCatalog.APPLICATION_STATE, VALID_UUID);
    }

    @Test
    void validate_throwsBusinessRuleExceptionWithoutRepositoryLookup_whenStateIsMalformedUuid() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().stateId(MALFORMED_UUID).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));

        verifyNoInteractions(recordExistsCatalogPort);
    }

    @Test
    void validate_throwsBusinessRuleException_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));

        verifyNoInteractions(recordExistsCatalogPort);
    }
}
