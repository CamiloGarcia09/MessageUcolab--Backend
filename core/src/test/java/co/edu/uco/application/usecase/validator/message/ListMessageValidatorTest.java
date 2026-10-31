package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListMessageValidatorTest {

    private static final String PAGE_RANGE_MESSAGE = "The page number must be greater than or equal to 1.";

    @Mock
    private SimplePageRequestCompositeValidator pageRequestValidator;

    @InjectMocks
    private ListMessageValidator validator;

    @Test
    void validate_delegatesToThePageCompositeUsingMessageDataAsModelClass() {
        SimplePageRequest request = new SimplePageRequest();
        request.setColumnSort("id");

        validator.validate(request);

        verify(pageRequestValidator).validate(request, MessageData.class);
    }

    @Test
    void validate_delegatesANullRequestToThePageComposite() {
        validator.validate(null);

        verify(pageRequestValidator).validate(null, MessageData.class);
    }

    @Test
    void validate_propagatesTheBusinessRule_whenThePageCompositeRejectsTheRequest() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(0);
        doThrow(BusinessRuleException.buildUserException(PAGE_RANGE_MESSAGE))
                .when(pageRequestValidator).validate(request, MessageData.class);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getUserMessage)
                        .isEqualTo(PAGE_RANGE_MESSAGE));
    }
}
