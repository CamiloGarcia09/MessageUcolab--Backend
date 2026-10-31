package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRuleExceptionTest {

    @Test
    void buildUserException_createsBusinessRuleType() {
        BusinessRuleException exception = BusinessRuleException.buildUserException("rule violation");
        assertThat(exception.getUserMessage()).isEqualTo("rule violation");
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS_RULE);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void buildTechnicalException_singleArg_createsTechnicalGeneral() {
        BusinessRuleException exception = BusinessRuleException.buildTechnicalException("technical");
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical");
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void buildTechnicalException_twoArgs_createsTechnicalApplication() {
        BusinessRuleException exception = BusinessRuleException.buildTechnicalException("user", "technical");
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
    }

    @Test
    void buildTechnicalException_withRootAndLocation_usesGivenLocation() {
        RuntimeException root = new RuntimeException("root");
        BusinessRuleException exception = BusinessRuleException.buildTechnicalException(
                "technical", root, ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getRootException()).isEqualTo(root);
    }

    @Test
    void twoArgConstructor_createsBusinessRuleApplication() {
        BusinessRuleException exception = new BusinessRuleException("user", "technical");
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS_RULE);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
    }

    @Test
    void fiveArgConstructor_createsExceptionWithGivenValues() {
        BusinessRuleException exception = new BusinessRuleException(
                "user", "technical", null,
                ExceptionType.BUSINESS, ExceptionLocation.APPLICATION);
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
        assertThat(exception.getUserMessage()).isEqualTo("user");
    }

    @Test
    void setType_defaultsNullToGeneral() {
        BusinessRuleException exception = BusinessRuleException.buildUserException("msg");
        exception.setType(null);
        assertThat(exception.getType()).isEqualTo(ExceptionType.GENERAL);
    }

    @Test
    void setLocation_defaultsNullToGeneral() {
        BusinessRuleException exception = BusinessRuleException.buildUserException("msg");
        exception.setLocation(null);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void extendsCrossWordsException() {
        assertThat(BusinessRuleException.class.getSuperclass()).isEqualTo(CrossWordsException.class);
    }
}