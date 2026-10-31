package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;


public class BusinessException extends CrossWordsException {
    private ExceptionType type;
    private ExceptionLocation location;
    protected BusinessException(String userMessage, String technicalMessage, Exception rootException, ExceptionLocation location) {
        super(userMessage, technicalMessage, rootException, ExceptionType.BUSINESS, location);
    }

    public BusinessException(String userMessage, String technicalMessage, Exception rootException, ExceptionType type, ExceptionLocation location) {
        super(userMessage, technicalMessage, rootException, type, location);
        setType(type);
        setLocation(location);
    }

    public BusinessException(String userMessage, String technicalMessage, Exception rootException) {
        super(userMessage, technicalMessage, rootException, ExceptionType.BUSINESS, ExceptionLocation.APPLICATION);
    }

    public static BusinessException buildUserException(String userMessage) {
        return new BusinessException(userMessage, userMessage, null, null);
    }

    public static BusinessException buildTechnicalException(String technicalMessage) {
        return new BusinessException(null, technicalMessage, null, ExceptionType.TECHNICAL, null);
    }

    public static BusinessException buildTechnicalException(String userMessage, String technicalMessage) {
        return new BusinessException(userMessage, technicalMessage, null, ExceptionType.TECHNICAL, ExceptionLocation.APPLICATION);
    }

    public static BusinessException buildTechnicalException(String technicalMessage, Exception rootException, ExceptionLocation location) {
        return new BusinessException(null, technicalMessage, rootException, ExceptionType.TECHNICAL, location);
    }
}
