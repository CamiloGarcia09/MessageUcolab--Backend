package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

public class ConflictException extends CrossWordsException {

    public ConflictException(String userMessage, String technicalMessage, Exception rootException, ExceptionType type, ExceptionLocation location) {
        super(userMessage, technicalMessage, rootException, type, location);
        setHttpStatus(409);
    }

    public ConflictException(String userMessage, String technicalMessage, Exception rootException) {
        super(userMessage, technicalMessage, rootException, ExceptionType.BUSINESS, ExceptionLocation.APPLICATION);
        setHttpStatus(409);
    }

    public static ConflictException buildUserException(String userMessage) {
        return new ConflictException(userMessage, userMessage, null);
    }

    public static ConflictException buildTechnicalException(String technicalMessage, Exception rootException) {
        return new ConflictException(null, technicalMessage, rootException, ExceptionType.TECHNICAL, ExceptionLocation.GENERAL);
    }
}