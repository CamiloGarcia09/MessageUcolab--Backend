package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundExceptionTest {

    @Test
    void buildUserException_createsNotFoundBusinessException() {
        NotFoundException exception = NotFoundException.buildUserException("Resource not found");

        assertThat(exception)
                .extracting(CrossWordsException::getUserMessage, CrossWordsException::getTechnicalMessage,
                        CrossWordsException::getType, CrossWordsException::getLocation,
                        CrossWordsException::getHttpStatus)
                .containsExactly("Resource not found", "Resource not found", ExceptionType.BUSINESS,
                        ExceptionLocation.APPLICATION, 404);
    }
}
