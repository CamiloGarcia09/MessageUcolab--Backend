package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedExceptionTest {

    @Test
    void buildUserException_createsUnauthorizedBusinessRule() {
        UnauthorizedException exception = UnauthorizedException.buildUserException("Invalid token");

        assertThat(exception)
                .extracting(CrossWordsException::getUserMessage, CrossWordsException::getTechnicalMessage,
                        CrossWordsException::getType, CrossWordsException::getLocation,
                        CrossWordsException::getHttpStatus)
                .containsExactly("Invalid token", "Invalid token", ExceptionType.BUSINESS_RULE,
                        ExceptionLocation.APPLICATION, 401);
    }
}
