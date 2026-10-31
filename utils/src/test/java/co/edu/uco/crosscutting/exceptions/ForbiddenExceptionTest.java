package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForbiddenExceptionTest {

    @Test
    void buildUserException_createsForbiddenBusinessRule() {
        ForbiddenException exception = ForbiddenException.buildUserException("Context forbidden");

        assertThat(exception)
                .extracting(CrossWordsException::getUserMessage, CrossWordsException::getTechnicalMessage,
                        CrossWordsException::getType, CrossWordsException::getLocation,
                        CrossWordsException::getHttpStatus)
                .containsExactly("Context forbidden", "Context forbidden", ExceptionType.BUSINESS_RULE,
                        ExceptionLocation.APPLICATION, 403);
    }
}
