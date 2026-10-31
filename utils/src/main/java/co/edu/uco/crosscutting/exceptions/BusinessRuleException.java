package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

public class BusinessRuleException extends CrossWordsException {
    protected BusinessRuleException(String userMessage, String technicalMessage, Exception rootException, ExceptionType type, ExceptionLocation location) {
        super(userMessage, technicalMessage, rootException, type, location);
    }

    public BusinessRuleException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage, null, ExceptionType.BUSINESS_RULE, ExceptionLocation.APPLICATION);
    }

    public static BusinessRuleException buildUserException(String userMessage) {
        return new BusinessRuleException(userMessage, userMessage, null, ExceptionType.BUSINESS_RULE, null);
    }

    public static BusinessRuleException buildTechnicalException(String technicalMessage) {
        return new BusinessRuleException(null, technicalMessage, null, ExceptionType.TECHNICAL, null);
    }

    public static BusinessRuleException buildTechnicalException(String userMessage, String technicalMessage) {
        return new BusinessRuleException(userMessage, technicalMessage, null, ExceptionType.TECHNICAL, ExceptionLocation.APPLICATION);
    }

    public static BusinessRuleException buildTechnicalException(String technicalMessage, Exception rootException, ExceptionLocation location) {
        return new BusinessRuleException(null, technicalMessage, rootException, ExceptionType.TECHNICAL, location);
    }
}
