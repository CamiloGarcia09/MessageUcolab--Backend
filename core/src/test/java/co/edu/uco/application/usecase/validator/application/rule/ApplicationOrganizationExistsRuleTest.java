package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationOrganizationExistsRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String MALFORMED_UUID = "not-a-uuid";
    private static final String NOT_FOUND_MESSAGE = "La organización no existe.";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private OrganizationRepository organizationRepository;

    private ApplicationOrganizationExistsRule rule;

    @BeforeEach
    void setUp() {
        lenient().when(catalogPort.getMessage(anyString())).thenReturn("user message");
        rule = new ApplicationOrganizationExistsRule(catalogPort, organizationRepository);
    }

    @Test
    void validate_doesNotThrow_whenOrganizationExists() {
        when(organizationRepository.findById(UUID.fromString(VALID_UUID)))
                .thenReturn(Optional.of(new OrganizationEntity()));
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(VALID_UUID).build();

        assertDoesNotThrow(() -> rule.validate(dto));

        verify(organizationRepository).findById(UUID.fromString(VALID_UUID));
    }

    @Test
    void validate_throwsBusinessRuleExceptionUsingCatalogCode_whenOrganizationDoesNotExist() {
        when(organizationRepository.findById(UUID.fromString(VALID_UUID)))
                .thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_151.getCode()))
                .thenReturn(NOT_FOUND_MESSAGE);
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(VALID_UUID).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getUserMessage())
                        .isEqualTo(NOT_FOUND_MESSAGE));

        verify(organizationRepository).findById(UUID.fromString(VALID_UUID));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_151.getCode());
    }

    @Test
    void validate_throwsBusinessRuleExceptionWithoutRepositoryLookup_whenOrganizationIdIsMalformedUuid() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(MALFORMED_UUID).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));

        verifyNoInteractions(organizationRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validate_throwsBusinessRuleExceptionWithoutRepositoryLookup_whenOrganizationIdIsBlank(
            String organizationId) {
        CreateApplicationDTO dto = CreateApplicationDTO.builder().organizationId(organizationId).build();

        assertThrows(BusinessRuleException.class, () -> rule.validate(dto));

        verifyNoInteractions(organizationRepository);
    }

    @Test
    void validate_throwsBusinessRuleExceptionWithoutRepositoryLookup_whenDtoIsNull() {
        assertThrows(BusinessRuleException.class, () -> rule.validate(null));

        verifyNoInteractions(organizationRepository);
    }
}
