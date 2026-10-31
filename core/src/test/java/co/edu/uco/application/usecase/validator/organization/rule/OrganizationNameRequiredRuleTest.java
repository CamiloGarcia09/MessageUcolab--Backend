package co.edu.uco.application.usecase.validator.organization.rule;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationNameRequiredRuleTest {

    private static final String REQUIRED_MESSAGE = "The organization name is required.";

    @Mock
    private CatalogPort catalogPort;

    private OrganizationNameRequiredRule rule;

    @BeforeEach
    void setUp() {
        rule = new OrganizationNameRequiredRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenNameIsPresent() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();

        assertThatCode(() -> rule.validate(dto)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "name=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void validate_throwsBusinessRuleUsingFun147_whenNameIsBlank(String name) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_147.getCode())).thenReturn(REQUIRED_MESSAGE);
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name(name).build();

        assertThatThrownBy(() -> rule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, REQUIRED_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_147.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun147_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_147.getCode())).thenReturn(REQUIRED_MESSAGE);

        assertThatThrownBy(() -> rule.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage).isEqualTo(REQUIRED_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_147.getCode());
    }
}
