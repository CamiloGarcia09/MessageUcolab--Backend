package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

@SuppressWarnings({"java:S110", "java:S9149"})
public final class UnauthorizedException extends BusinessRuleException {

    private UnauthorizedException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage, null, ExceptionType.BUSINESS_RULE, ExceptionLocation.APPLICATION);
        setHttpStatus(401);
    }

    public static UnauthorizedException buildUserException(String userMessage) {
        return new UnauthorizedException(userMessage, userMessage);
    }
}
