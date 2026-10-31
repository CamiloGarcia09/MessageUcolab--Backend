package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static co.edu.uco.application.CrosswordsConstant.REQUEST_PAGE_DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageNumberRuleTest {

    private static final String PAGE_RANGE_MESSAGE = "The page number must be greater than or equal to 1.";

    @Mock
    private CatalogPort catalogPort;

    private PageNumberRule rule;

    @BeforeEach
    void setUp() {
        rule = new PageNumberRule(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 50, Integer.MAX_VALUE})
    void validate_doesNotThrow_whenPageIsGreaterThanOrEqualToTheDefaultPage(int page) {
        SimplePageRequestValidationContext context = contextWithPage(page);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_acceptsTheDefaultPageBoundary() {
        SimplePageRequestValidationContext context = contextWithPage(REQUEST_PAGE_DEFAULT);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void validate_throwsBusinessRuleUsingFun032_whenPageIsBelowTheDefaultPage(int page) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_032.getCode())).thenReturn(PAGE_RANGE_MESSAGE);
        SimplePageRequestValidationContext context = contextWithPage(page);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, PAGE_RANGE_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_032.getCode());
    }

    @Nested
    class NullGuardTest {

        @Test
        void validate_throwsBusinessRuleUsingFun032_whenTheContextIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_032.getCode())).thenReturn(PAGE_RANGE_MESSAGE);

            assertThatThrownBy(() -> rule.validate(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, PAGE_RANGE_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_032.getCode());
        }

        @Test
        void validate_throwsBusinessRuleUsingFun032_whenTheRequestIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_032.getCode())).thenReturn(PAGE_RANGE_MESSAGE);
            SimplePageRequestValidationContext context =
                    new SimplePageRequestValidationContext(null, SimplePageRequest.class);

            assertThatThrownBy(() -> rule.validate(context))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, PAGE_RANGE_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_032.getCode());
        }
    }

    private SimplePageRequestValidationContext contextWithPage(int page) {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(page);
        return new SimplePageRequestValidationContext(request, SimplePageRequest.class);
    }
}
