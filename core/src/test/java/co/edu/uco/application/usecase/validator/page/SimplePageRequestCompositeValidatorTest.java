package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimplePageRequestCompositeValidatorTest {

    private static final String PAGE_RANGE_MESSAGE = "The page number must be greater than or equal to 1.";
    private static final String PAGE_SIZE_PATTERN = "Page size must be between 1 and %d.";
    private static final String SORT_DIRECTION_MESSAGE = "Sort direction must be ASC or DESC.";
    private static final String SORT_COLUMN_PATTERN = "Sort column %s does not exist in the model.";
    private static final String UNKNOWN_COLUMN = "unknownColumn";

    @Mock
    private CatalogPort catalogPort;

    private SimplePageRequestCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SimplePageRequestCompositeValidator(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenEveryRuleIsSatisfied() {
        SimplePageRequest request = new SimplePageRequest();

        assertThatCode(() -> validator.validate(request, MessageData.class)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50, 100})
    void validate_doesNotThrow_whenSizeIsOnTheAllowedRange(int size) {
        SimplePageRequest request = new SimplePageRequest();
        request.setSize(size);

        assertThatCode(() -> validator.validate(request, MessageData.class)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenColumnSortBelongsToTheInformedModelClass() {
        SimplePageRequest request = new SimplePageRequest();
        request.setColumnSort("title");

        assertThatCode(() -> validator.validate(request, MessageData.class)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsPageSizeSortDirectionAndSortColumnChecks_whenPageIsBelowTheDefaultPage() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_032.getCode())).thenReturn(PAGE_RANGE_MESSAGE);
        SimplePageRequest request = invalidRequestEverywhere();
        request.setPage(0);

        assertThatThrownBy(() -> validator.validate(request, MessageData.class))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, PAGE_RANGE_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_032.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsSortDirectionAndSortColumnChecks_whenSizeIsOutsideTheAllowedRange() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_029.getCode())).thenReturn(PAGE_SIZE_PATTERN);
        SimplePageRequest request = invalidRequestEverywhere();
        request.setPage(1);
        request.setSize(101);

        assertThatThrownBy(() -> validator.validate(request, MessageData.class))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "Page size must be between 1 and 100."));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_029.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsSortColumnCheck_whenSortDirectionIsNotSupported() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_031.getCode())).thenReturn(SORT_DIRECTION_MESSAGE);
        SimplePageRequest request = invalidRequestEverywhere();
        request.setPage(1);
        request.setSize(50);

        assertThatThrownBy(() -> validator.validate(request, MessageData.class))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, SORT_DIRECTION_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_031.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun030_whenColumnSortIsNotDeclaredByTheModelClass() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_030.getCode())).thenReturn(SORT_COLUMN_PATTERN);
        SimplePageRequest request = invalidRequestEverywhere();
        request.setPage(1);
        request.setSize(50);
        request.setSort("ASC");

        assertThatThrownBy(() -> validator.validate(request, MessageData.class))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, String.format(SORT_COLUMN_PATTERN, UNKNOWN_COLUMN)));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_030.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    @Test
    void validate_shortCircuitsEveryRule_whenRequestIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_010.getCode())).thenReturn("data is null");

        assertThatThrownBy(() -> validator.validate(null, MessageData.class))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, "data is null"));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_010.getCode());
        verifyNoMoreInteractions(catalogPort);
    }

    private SimplePageRequest invalidRequestEverywhere() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(0);
        request.setSize(0);
        request.setSort("SIDEWAYS");
        request.setColumnSort(UNKNOWN_COLUMN);
        return request;
    }
}
