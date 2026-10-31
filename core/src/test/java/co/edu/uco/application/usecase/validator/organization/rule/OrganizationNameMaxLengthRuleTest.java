package co.edu.uco.application.usecase.validator.organization.rule;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationNameMaxLengthRuleTest {

    private static final String MAX_LENGTH_MESSAGE = "The organization name cannot exceed 50 characters.";
    private static final String FIFTY_CHARS = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
    private static final String FIFTY_ONE_CHARS = FIFTY_CHARS + "a";

    @Mock
    private CatalogPort catalogPort;

    private OrganizationNameMaxLengthRule rule;

    @BeforeEach
    void setUp() {
        rule = new OrganizationNameMaxLengthRule(catalogPort);
        assertAll(
                () -> assertThat(FIFTY_CHARS).hasSize(50),
                () -> assertThat(FIFTY_ONE_CHARS).hasSize(51));
    }

    @Test
    void validate_doesNotThrow_whenNameIsExactlyFiftyCharacters() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(FIFTY_CHARS).build();

        assertThatCode(() -> rule.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_doesNotThrow_whenSurroundingSpacesAreTrimmedBeforeMeasuringTheLimit() {
        CreateOrganizationDTO dto = new CreateOrganizationDTO();
        dto.setName("  " + FIFTY_CHARS + "  ");

        assertThatCode(() -> rule.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_doesNotThrow_whenNameIsShorterThanTheLimit() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();

        assertThatCode(() -> rule.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsBusinessRuleUsingFun148_whenNameExceedsFiftyCharacters() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_148.getCode()))
                .thenReturn(MAX_LENGTH_MESSAGE);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(FIFTY_ONE_CHARS).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, MAX_LENGTH_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_148.getCode());
    }

    @ParameterizedTest(name = "dto=[{0}]")
    @ValueSource(strings = {" ", "   "})
    void validate_doesNotThrow_whenNameOnlyContainsBlankText(String name) {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(name).build();

        assertThatCode(() -> rule.validate(dto)).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsBusinessRule_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_148.getCode()))
                .thenReturn(MAX_LENGTH_MESSAGE);

        assertThatThrownBy(() -> rule.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage).isEqualTo(MAX_LENGTH_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_148.getCode());
    }
}
