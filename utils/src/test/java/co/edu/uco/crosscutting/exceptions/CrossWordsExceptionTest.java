package co.edu.uco.crosscutting.exceptions;

import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrossWordsExceptionTest {

    @Test
    void build_singleMessage_createsTechnicalGeneralException() {
        CrossWordsException exception = CrossWordsException.build("technical failure");
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical failure");
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
        assertThat(exception.getUserMessage()).isEmpty();
    }

    @Test
    void build_withRootException_keepsRootException() {
        RuntimeException root = new RuntimeException("root");
        CrossWordsException exception = CrossWordsException.build("technical", root);
        assertThat(exception.getRootException()).isEqualTo(root);
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
    }

    @Test
    void build_withFullArguments_setsAllFields() {
        RuntimeException root = new RuntimeException("root");
        CrossWordsException exception = CrossWordsException.build(
                "technical", "user", root, ExceptionType.BUSINESS, ExceptionLocation.APPLICATION);
        assertThat(exception.getTechnicalMessage()).isEqualTo("technical");
        assertThat(exception.getUserMessage()).isEqualTo("user");
        assertThat(exception.getRootException()).isEqualTo(root);
        assertThat(exception.getType()).isEqualTo(ExceptionType.BUSINESS);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.APPLICATION);
    }

    @Test
    void buildInfrastructure_usesInfrastructureLocation() {
        RuntimeException root = new RuntimeException("root");
        CrossWordsException exception = CrossWordsException.buildInfrastructure(
                "technical", "user", root, ExceptionType.TECHNICAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getUserMessage()).isEqualTo("user");
    }

    @Test
    void buildInfrastructure_shortForm_createsTechnicalInfrastructure() {
        RuntimeException root = new RuntimeException("root");
        CrossWordsException exception = CrossWordsException.buildInfrastructure("technical", root);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getType()).isEqualTo(ExceptionType.TECHNICAL);
        assertThat(exception.getRootException()).isEqualTo(root);
    }

    @Test
    void buildInfrastructure_withoutRoot_usesEmptyExceptionAsRoot() {
        CrossWordsException exception = CrossWordsException.buildInfrastructure(
                "technical", "user", ExceptionType.GENERAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE);
        assertThat(exception.getRootException()).isNotNull();
    }

    @Test
    void constructor_trimsUserMessage() {
        CrossWordsException exception = new CrossWordsException("  padded message  ", "technical", null);
        assertThat(exception.getUserMessage()).isEqualTo("padded message");
    }

    @Test
    void constructor_doesNotTrimTechnicalMessage() {
        CrossWordsException exception = new CrossWordsException("user", "  technical  ", null);
        assertThat(exception.getTechnicalMessage()).isEqualTo("  technical  ");
    }

    @Test
    void constructor_defaultsTypeAndLocationToGeneral() {
        CrossWordsException exception = new CrossWordsException("user", "technical", null);
        assertThat(exception.getType()).isEqualTo(ExceptionType.GENERAL);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void constructor_setsRootExceptionToNewException_whenNull() {
        CrossWordsException exception = new CrossWordsException("user", "technical", null);
        assertThat(exception.getRootException()).isNotNull();
    }

    @Test
    void setType_defaultsNullToGeneral() {
        CrossWordsException exception = new CrossWordsException("user", "technical", null);
        exception.setType(null);
        assertThat(exception.getType()).isEqualTo(ExceptionType.GENERAL);
    }

    @Test
    void setLocation_defaultsNullToGeneral() {
        CrossWordsException exception = new CrossWordsException("user", "technical", null);
        exception.setLocation(null);
        assertThat(exception.getLocation()).isEqualTo(ExceptionLocation.GENERAL);
    }

    @Test
    void isRuntimeException() {
        assertThat(RuntimeException.class).isAssignableFrom(CrossWordsException.class);
    }
}