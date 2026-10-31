package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageRequestRangeValidatorTest {

    private static final String PAGE_RANGE_PATTERN = "Requested page exceeds total pages: %d";

    @Mock
    private CatalogPort catalogPort;

    private PageRequestRangeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PageRequestRangeValidator(catalogPort);
    }

    @ParameterizedTest
    @CsvSource({"2, 5", "1, 5", "5, 5", "0, 0", "3, 3"})
    void validate_doesNotThrow_whenPageIsWithinTheTotalPages(int page, int totalPages) {
        assertThatCode(() -> validator.validate(page, totalPages)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun028_whenPageExceedsTheTotalPages() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_028.getCode())).thenReturn(PAGE_RANGE_PATTERN);

        assertThatThrownBy(() -> validator.validate(6, 5))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "Requested page exceeds total pages: 5"));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_028.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun028_whenTheValidationContextCarriesAnExceededPage() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_028.getCode())).thenReturn(PAGE_RANGE_PATTERN);
        PageRequestRangeValidationContext context = new PageRequestRangeValidationContext(4, 2);

        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "Requested page exceeds total pages: 2"));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_028.getCode());
    }

    @Test
    void validate_doesNotThrow_whenTheValidationContextCarriesAValidPage() {
        PageRequestRangeValidationContext context = new PageRequestRangeValidationContext(2, 7);

        assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Nested
    class NullGuardTest {

        @Test
        void validate_throwsBusinessRuleUsingFun028_whenTheValidationContextIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_028.getCode())).thenReturn(PAGE_RANGE_PATTERN);

            assertThatThrownBy(() -> validator.validate((PageRequestRangeValidationContext) null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, String.format(PAGE_RANGE_PATTERN, 0)));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_028.getCode());
        }
    }
}
