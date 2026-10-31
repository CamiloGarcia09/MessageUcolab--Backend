package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

public class NotFoundException extends CrossWordsException {

    public NotFoundException(String userMessage, String technicalMessage, Exception rootException, ExceptionType type, ExceptionLocation location) {
        super(userMessage, technicalMessage, rootException, type, location);
        setHttpStatus(404);
    }

    public NotFoundException(String userMessage, String technicalMessage, Exception rootException) {
        super(userMessage, technicalMessage, rootException, ExceptionType.BUSINESS, ExceptionLocation.APPLICATION);
        setHttpStatus(404);
    }

    public static NotFoundException buildUserException(String userMessage) {
        return new NotFoundException(userMessage, userMessage, null);
    }

    public static NotFoundException buildTechnicalException(String technicalMessage, Exception rootException) {
        return new NotFoundException(null, technicalMessage, rootException, ExceptionType.TECHNICAL, ExceptionLocation.GENERAL);
    }
}