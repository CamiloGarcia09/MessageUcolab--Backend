package co.edu.uco.application.usecase.validator.organization.rule;

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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationNameDuplicatedRuleTest {

    private static final String NAME = "UCO";
    private static final String DUPLICATE_MESSAGE = "An organization with the given name already exists.";

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private CatalogPort catalogPort;

    private OrganizationNameDuplicatedRule rule;

    @BeforeEach
    void setUp() {
        rule = new OrganizationNameDuplicatedRule(catalogPort, organizationRepository);
    }

    @Test
    void validate_doesNotThrow_whenNameIsNotTakenYet() {
        when(organizationRepository.findByName(NAME)).thenReturn(Optional.empty());

        assertThatCode(() -> rule.validate(CreateOrganizationDTO.builder().name(NAME).build()))
                .doesNotThrowAnyException();

        verify(organizationRepository).findByName(NAME);
    }

    @Test
    void validate_doesNotThrow_whenFiftyCharacterNameIsNotTakenYet() {
        String boundaryName = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
        when(organizationRepository.findByName(boundaryName)).thenReturn(Optional.empty());

        assertThatCode(() -> rule.validate(CreateOrganizationDTO.builder().name(boundaryName).build()))
                .doesNotThrowAnyException();

        verify(organizationRepository).findByName(boundaryName);
    }

    @Test
    void validate_queriesTheNameComingFromTheDto() {
        CreateOrganizationDTO dto = new CreateOrganizationDTO();
        dto.setName("  UCO  ");
        when(organizationRepository.findByName(NAME)).thenReturn(Optional.empty());

        rule.validate(dto);

        verify(organizationRepository).findByName(NAME);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun149_whenNameIsAlreadyTaken() {
        when(organizationRepository.findByName(NAME)).thenReturn(Optional.of(new OrganizationEntity()));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_149.getCode()))
                .thenReturn(DUPLICATE_MESSAGE);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(NAME).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, DUPLICATE_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_149.getCode());
    }

    @Test
    void validate_throwsBusinessRule_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_149.getCode()))
                .thenReturn(DUPLICATE_MESSAGE);

        assertThatThrownBy(() -> rule.validate(null)).isInstanceOf(BusinessRuleException.class);
    }
}
