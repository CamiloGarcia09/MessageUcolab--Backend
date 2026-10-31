package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

@SuppressWarnings({"java:S110", "java:S9149"})
public final class ForbiddenException extends BusinessRuleException {

    private ForbiddenException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage, null, ExceptionType.BUSINESS_RULE, ExceptionLocation.APPLICATION);
        setHttpStatus(403);
    }

    public static ForbiddenException buildUserException(String userMessage) {
        return new ForbiddenException(userMessage, userMessage);
    }
}
