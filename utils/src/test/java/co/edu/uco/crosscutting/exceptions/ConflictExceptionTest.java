package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConflictExceptionTest {

    @Test
    void buildUserException_createsConflictBusinessException() {
        ConflictException exception = ConflictException.buildUserException("Resource conflict");

        assertThat(exception)
                .extracting(CrossWordsException::getUserMessage, CrossWordsException::getTechnicalMessage,
                        CrossWordsException::getType, CrossWordsException::getLocation,
                        CrossWordsException::getHttpStatus)
                .containsExactly("Resource conflict", "Resource conflict", ExceptionType.BUSINESS,
                        ExceptionLocation.APPLICATION, 409);
    }
}
