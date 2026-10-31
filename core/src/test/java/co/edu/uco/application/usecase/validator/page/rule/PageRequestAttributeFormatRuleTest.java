package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static co.edu.uco.application.CrosswordsConstant.COLUMN_SORT_ATTRIBUTE;
import static co.edu.uco.application.CrosswordsConstant.PAGE_ATTRIBUTE;
import static co.edu.uco.crosscutting.helpers.UtilText.ONLY_LETTERS;
import static co.edu.uco.crosscutting.helpers.UtilText.ONLY_NUMBERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageRequestAttributeFormatRuleTest {

    private static final String NUMERIC_PATTERN_MESSAGE = "The attribute %s must be numeric.";
    private static final String ALPHABETIC_PATTERN_MESSAGE = "The attribute %s must be alphabetic.";
    private static final String FORMATTED_NUMERIC_MESSAGE = "The attribute page must be numeric.";
    private static final String FORMATTED_ALPHABETIC_MESSAGE = "The attribute columnSort must be alphabetic.";

    @Mock
    private CatalogPort catalogPort;

    private PageRequestAttributeFormatRule numericRule;
    private PageRequestAttributeFormatRule alphabeticRule;

    @BeforeEach
    void setUp() {
        numericRule = new PageRequestAttributeFormatRule(
                catalogPort, PageRequestDTO::getPage, ONLY_NUMBERS, PAGE_ATTRIBUTE,
                MessageCatalogCodeEnum.FUN_033);
        alphabeticRule = new PageRequestAttributeFormatRule(
                catalogPort, PageRequestDTO::getColumnSort, ONLY_LETTERS, COLUMN_SORT_ATTRIBUTE,
                MessageCatalogCodeEnum.FUN_043);
    }

    @ParameterizedTest(name = "page=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"1", "50", "123"})
    void validate_doesNotThrow_whenNumericAttributeMatchesThePatternOrIsAbsent(String value) {
        PageRequestDTO dto = PageRequestDTO.builder().page(value).build();

        assertThatCode(() -> numericRule.validate(dto)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "columnSort", "ASC"})
    void validate_doesNotThrow_whenAlphabeticAttributeMatchesThePattern(String value) {
        PageRequestDTO dto = PageRequestDTO.builder().columnSort(value).build();

        assertThatCode(() -> alphabeticRule.validate(dto)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "1.5", "-1"})
    void validate_throwsBusinessRuleUsingFun033_whenNumericAttributeDoesNotMatchThePattern(String value) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_033.getCode()))
                .thenReturn(NUMERIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder().page(value).build();

        assertThatThrownBy(() -> numericRule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FORMATTED_NUMERIC_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_033.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "id2", "column sort"})
    void validate_throwsBusinessRuleUsingFun043_whenAlphabeticAttributeDoesNotMatchThePattern(String value) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_043.getCode()))
                .thenReturn(ALPHABETIC_PATTERN_MESSAGE);
        PageRequestDTO dto = PageRequestDTO.builder().columnSort(value).build();

        assertThatThrownBy(() -> alphabeticRule.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FORMATTED_ALPHABETIC_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_043.getCode());
    }

    @Nested
    class NullGuardTest {

        @Test
        void validate_throwsBusinessRuleUsingFun033_whenTheNumericCandidateIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_033.getCode()))
                    .thenReturn(NUMERIC_PATTERN_MESSAGE);

            assertThatThrownBy(() -> numericRule.validate(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, FORMATTED_NUMERIC_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_033.getCode());
        }

        @Test
        void validate_throwsBusinessRuleUsingFun043_whenTheAlphabeticCandidateIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_043.getCode()))
                    .thenReturn(ALPHABETIC_PATTERN_MESSAGE);

            assertThatThrownBy(() -> alphabeticRule.validate(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, FORMATTED_ALPHABETIC_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_043.getCode());
        }
    }
}
