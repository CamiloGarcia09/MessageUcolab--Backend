package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class SortDirectionRuleTest {

    private static final String SORT_DIRECTION_MESSAGE = "Sort direction must be ASC or DESC.";

    @Mock
    private CatalogPort catalogPort;

    private SortDirectionRule rule;

    @BeforeEach
    void setUp() {
        rule = new SortDirectionRule(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASC", "DESC", "asc", "desc", "Asc", "DeSc"})
    void validate_doesNotThrow_whenSortDirectionIsAscendingOrDescendingIgnoringCase(String sort) {
        SimplePageRequestValidationContext context = contextWithSort(sort);

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIDEWAYS", "ASCENDING", "1", ""})
    void validate_throwsBusinessRuleUsingFun031_whenSortDirectionIsNotSupported(String sort) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_031.getCode())).thenReturn(SORT_DIRECTION_MESSAGE);
        SimplePageRequestValidationContext context = contextWithSort(sort);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, SORT_DIRECTION_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_031.getCode());
    }

    @Nested
    class NullGuardTest {

        @ParameterizedTest(name = "{0}")
        @MethodSource("nullRelevantInputs")
        void validate_throwsBusinessRuleUsingFun031_whenContextRequestOrSortIsNull(
                String scenario, SimplePageRequestValidationContext context) {
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_031.getCode())).thenReturn(SORT_DIRECTION_MESSAGE);

            assertThatThrownBy(() -> rule.validate(context))
                    .as(scenario)
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(exception -> assertThat((BusinessRuleException) exception)
                            .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                            .containsExactly(422, SORT_DIRECTION_MESSAGE));
            verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_031.getCode());
        }

        static Stream<Arguments> nullRelevantInputs() {
            SimplePageRequest requestWithoutSort = new SimplePageRequest();
            requestWithoutSort.setSort(null);
            return Stream.of(
                    Arguments.of("nullContext", null),
                    Arguments.of("nullRequest", new SimplePageRequestValidationContext(null, SimplePageRequest.class)),
                    Arguments.of("nullSort",
                            new SimplePageRequestValidationContext(requestWithoutSort, SimplePageRequest.class)));
        }
    }

    private SimplePageRequestValidationContext contextWithSort(String sort) {
        SimplePageRequest request = new SimplePageRequest();
        request.setSort(sort);
        return new SimplePageRequestValidationContext(request, SimplePageRequest.class);
    }
}
