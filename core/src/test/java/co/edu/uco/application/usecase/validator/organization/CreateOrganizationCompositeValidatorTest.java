package co.edu.uco.application.usecase.validator.organization;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrganizationCompositeValidatorTest {

    private static final String NAME = "UCO";
    private static final String FIFTY_CHARS = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
    private static final String FIFTY_ONE_CHARS = FIFTY_CHARS + "a";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private OrganizationRepository organizationRepository;

    private CreateOrganizationCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CreateOrganizationCompositeValidator(catalogPort, organizationRepository);
    }

    @Test
    void validate_doesNotThrow_whenNameIsAvailableAndWithinTheLimit() {
        when(organizationRepository.findByName(NAME)).thenReturn(Optional.empty());

        validator.validate(CreateOrganizationDTO.builder().name(NAME).build());

        verify(organizationRepository).findByName(NAME);
    }

    @Test
    void validate_shortCircuitsEveryRule_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_010.getCode()))
                .thenReturn("Invalid data");

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage).isEqualTo("Invalid data"));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_010.getCode());
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void validate_shortCircuitsMaxLengthAndDuplicateChecks_whenNameIsBlank() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_147.getCode()))
                .thenReturn("The organization name is required.");
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("").build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage)
                        .isEqualTo("The organization name is required."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_147.getCode());
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void validate_shortCircuitsDuplicateCheck_whenNameExceedsFiftyCharacters() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_148.getCode()))
                .thenReturn("The organization name cannot exceed 50 characters.");
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(FIFTY_ONE_CHARS).build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage)
                        .isEqualTo("The organization name cannot exceed 50 characters."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_148.getCode());
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun149_whenNameIsAlreadyTaken() {
        when(organizationRepository.findByName(NAME)).thenReturn(Optional.of(new OrganizationEntity()));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_149.getCode()))
                .thenReturn("An organization with the given name already exists.");
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(NAME).build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "An organization with the given name already exists."));
        verify(organizationRepository).findByName(NAME);
    }

    @Test
    void validate_acceptsNameAtTheFiftyCharacterBoundary() {
        when(organizationRepository.findByName(FIFTY_CHARS)).thenReturn(Optional.empty());

        validator.validate(CreateOrganizationDTO.builder().name(FIFTY_CHARS).build());

        verify(organizationRepository).findByName(FIFTY_CHARS);
    }
}
