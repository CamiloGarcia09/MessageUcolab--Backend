package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SortColumnRuleTest {

    private static final String SORT_COLUMN_PATTERN = "Sort column %s does not exist in the model.";
    private static final String UNKNOWN_COLUMN = "unknownColumn";
    private static final String FORMATTED_SORT_COLUMN_MESSAGE =
            "Sort column unknownColumn does not exist in the model.";

    @Mock
    private CatalogPort catalogPort;

    private SortColumnRule rule;

    @BeforeEach
    void setUp() {
        rule = new SortColumnRule(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "code", "title", "content"})
    void validate_doesNotThrow_whenColumnSortIsDeclaredByTheModelClass(String columnSort) {
        SimplePageRequestValidationContext context = context(columnSort, MessageData.class);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenColumnSortBelongsToTheInformedModelClass() {
        SimplePageRequestValidationContext context = context("page", SimplePageRequest.class);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun030_whenColumnSortIsNotDeclaredByTheModelClass() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_030.getCode())).thenReturn(SORT_COLUMN_PATTERN);
        SimplePageRequestValidationContext context = context(UNKNOWN_COLUMN, MessageData.class);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, FORMATTED_SORT_COLUMN_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_030.getCode());
    }

    @Nested
    class NullGuardTest {

        @ParameterizedTest(name = "{0}")
        @MethodSource("nullRelevantInputs")
        void validate_throwsBusinessRuleUsingFun030_whenContextRequestModelClassOrColumnSortIsNull(
                String scenario, SimplePageRequestValidationContext context, String expectedColumnSort) {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_030.getCode())).thenReturn(SORT_COLUMN_PATTERN);

            assertThatThrownBy(() -> rule.validate(context))
                    .as(scenario)
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, String.format(SORT_COLUMN_PATTERN, expectedColumnSort)));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_030.getCode());
        }

        static Stream<Arguments> nullRelevantInputs() {
            SimplePageRequest requestWithoutColumnSort = new SimplePageRequest();
            requestWithoutColumnSort.setColumnSort(null);
            SimplePageRequest requestWithColumnSort = new SimplePageRequest();
            requestWithColumnSort.setColumnSort("id");
            return Stream.of(
                    Arguments.of("nullContext", null, null),
                    Arguments.of("nullRequest",
                            new SimplePageRequestValidationContext(null, MessageData.class), null),
                    Arguments.of("nullModelClass",
                            new SimplePageRequestValidationContext(requestWithColumnSort, null), "id"),
                    Arguments.of("nullColumnSort",
                            new SimplePageRequestValidationContext(requestWithoutColumnSort, MessageData.class),
                            null));
        }
    }

    private SimplePageRequestValidationContext context(String columnSort, Class<?> modelClass) {
        SimplePageRequest request = new SimplePageRequest();
        request.setColumnSort(columnSort);
        return new SimplePageRequestValidationContext(request, modelClass);
    }
}
