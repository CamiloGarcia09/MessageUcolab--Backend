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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageSizeRuleTest {

    private static final String PAGE_SIZE_PATTERN = "Page size must be between 1 and %d.";
    private static final String FORMATTED_PAGE_SIZE_MESSAGE = "Page size must be between 1 and 100.";

    @Mock
    private CatalogPort catalogPort;

    private PageSizeRule rule;

    @BeforeEach
    void setUp() {
        rule = new PageSizeRule(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 25, 50, 100})
    void validate_doesNotThrow_whenSizeIsWithinTheAllowedRange(int size) {
        SimplePageRequestValidationContext context = contextWithSize(size);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 500})
    void validate_throwsBusinessRuleUsingFun029_whenSizeIsOutsideTheAllowedRange(int size) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_029.getCode())).thenReturn(PAGE_SIZE_PATTERN);
        SimplePageRequestValidationContext context = contextWithSize(size);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FORMATTED_PAGE_SIZE_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_029.getCode());
    }

    @Nested
    class NullGuardTest {

        @Test
        void validate_throwsBusinessRuleUsingFun029_whenTheContextIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_029.getCode())).thenReturn(PAGE_SIZE_PATTERN);

            assertThatThrownBy(() -> rule.validate(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, FORMATTED_PAGE_SIZE_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_029.getCode());
        }

        @Test
        void validate_throwsBusinessRuleUsingFun029_whenTheRequestIsNull() {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_029.getCode())).thenReturn(PAGE_SIZE_PATTERN);
            SimplePageRequestValidationContext context =
                    new SimplePageRequestValidationContext(null, SimplePageRequest.class);

            assertThatThrownBy(() -> rule.validate(context))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, FORMATTED_PAGE_SIZE_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_029.getCode());
        }
    }

    private SimplePageRequestValidationContext contextWithSize(int size) {
        SimplePageRequest request = new SimplePageRequest();
        request.setSize(size);
        return new SimplePageRequestValidationContext(request, SimplePageRequest.class);
    }
}
