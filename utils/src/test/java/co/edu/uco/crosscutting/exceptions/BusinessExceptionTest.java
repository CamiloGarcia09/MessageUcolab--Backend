package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void buildUserException_createsBusinessTypeWithUserMessage() {
        BusinessException exception = BusinessException.buildUserException("user message");
        assertThat(exception.getUserMessage()).isEqualTo("user message");
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void buildTechnicalException_singleArg_createsTechnicalGeneral() {
        BusinessException exception = BusinessException.buildTechnicalException("technical");
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical");
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void buildTechnicalException_twoArgs_createsTechnicalApplication() {
        BusinessException exception = BusinessException.buildTechnicalException("user", "technical");
        assertThat(exception.getUserMessage()).isEqualTo("user");
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical");
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
    }

    @Test
    void buildTechnicalException_withRootAndLocation_usesGivenLocation() {
        RuntimeException root = new RuntimeException("root");
        BusinessException exception = BusinessException.buildTechnicalException(
                "technical", root, ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical");
        assertThat(exception.getRootException()).isEqualTo(root);
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE);
    }

    @Test
    void threeArgConstructor_usesBusinessApplicationDefaults() {
        RuntimeException root = new RuntimeException("root");
        BusinessException exception = new BusinessException("user", "technical", root);
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
        assertThat(exception.getRootException()).isEqualTo(root);
    }

    @Test
    void fullConstructor_defaultsNullTypeToGeneral() {
        BusinessException exception = new BusinessException(
                "user", "technical", null, null, ExceptionLocation.APPLICATION);
        assertThat(exception.getType()).isEqualTo(ExceptionType.GENERAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
    }

    @Test
    void extendsCrossWordsException() {
        assertThat(BusinessException.class.getSuperclass()).isEqualTo(CrossWordsException.class);
    }
}