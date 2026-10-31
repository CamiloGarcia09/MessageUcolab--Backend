package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static co.edu.uco.application.CrosswordsConstant.COLUMN_SORT_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.PAGE_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.SIZE_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.SORT_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageRequestDTOCompositeValidatorTest {

    private static final String NUMERIC_PATTERN_MESSAGE = "El atributo %s debe ser numerico";
    private static final String ALPHABETIC_PATTERN_MESSAGE = "El atributo %s debe ser alfabetico";

    @Mock
    private CatalogPort catalogPort;

    private PageRequestDTOCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PageRequestDTOCompositeValidator(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenEveryAttributeMatchesItsPattern() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("1").size("50").columnSort("id").sort("asc").build();

        assertThatCode(() -> validator.validate(dto)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenEveryAttributeIsNull() {
        PageRequestDTO dto = new PageRequestDTO();

        assertThatCode(() -> validator.validate(dto)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsEveryAttributeCheck_whenDataIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_010.getCode())).thenReturn("data is null");

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "data is null"));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_010.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsSizeColumnSortAndSortChecks_whenPageIsNotNumeric() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_033.getCode()))
                .thenReturn(NUMERIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("abc").size("abc").columnSort("123").sort("123").build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, String.format(NUMERIC_PATTERN_MESSAGE, PAGE_ATTRIBUTE)));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_033.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsColumnSortAndSortChecks_whenSizeIsNotNumeric() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_033.getCode()))
                .thenReturn(NUMERIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("1").size("abc").columnSort("123").sort("123").build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, String.format(NUMERIC_PATTERN_MESSAGE, SIZE_ATTRIBUTE)));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_033.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsSortCheck_whenColumnSortIsNotAlphabetic() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_043.getCode()))
                .thenReturn(ALPHABETIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("1").size("50").columnSort("123").sort("123").build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, String.format(ALPHABETIC_PATTERN_MESSAGE, COLUMN_SORT_ATTRIBUTE)));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_043.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun043_whenSortIsNotAlphabetic() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_043.getCode()))
                .thenReturn(ALPHABETIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("1").size("50").columnSort("id").sort("123").build();

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, String.format(ALPHABETIC_PATTERN_MESSAGE, SORT_ATTRIBUTE)));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_043.getCode());
        verifyNoMoreInteractions(catalogPort);
    }
}
