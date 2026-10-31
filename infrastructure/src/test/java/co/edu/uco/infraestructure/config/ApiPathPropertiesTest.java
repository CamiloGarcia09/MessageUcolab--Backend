package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ApiPathPropertiesTest {

    private Properties apiPaths;

    @BeforeEach
    void loadApiPaths() {
        apiPaths = new Properties();
        try (InputStream input = ApiPathPropertiesTest.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            assertThat(input).as("application.properties must be on the test classpath").isNotNull();
            apiPaths.load(input);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    void messagesPath_pointsToTheV1MessagesResource() {
        assertThat(apiPaths.getProperty("crosswords.api.path.messages"))
                .isEqualTo("/messageucolab/v1/messages");
    }

    @Test
    void applicationPath_pointsToTheV1ApplicationResource() {
        assertThat(apiPaths.getProperty("crosswords.api.path.application"))
                .isEqualTo("/messageucolab/v1/application");
    }

    @Test
    void legacyMessagePaths_areNoLongerDeclared() {
        assertAll(
                () -> assertThat(apiPaths.getProperty("crosswords.api.path.message")).isNull(),
                () -> assertThat(apiPaths.getProperty("crosswords.api.path.message.environment")).isNull(),
                () -> assertThat(apiPaths.getProperty("crosswords.api.path.message.code.environment")).isNull(),
                () -> assertThat(apiPaths.getProperty("crosswords.api.path.message.code.translation")).isNull());
    }

    @Test
    void manualEnvironmentCreationPath_isNoLongerDeclared() {
        List<String> environmentPaths = apiPaths.stringPropertyNames().stream()
                .filter(key -> key.startsWith("crosswords.api.path.") && key.endsWith(".environment"))
                .toList();

        assertThat(environmentPaths)
                .as("the manual environment creation route was removed from the API contract")
                .isEmpty();
        assertThat(apiPaths.getProperty("crosswords.api.path.environment")).isNull();
    }
}
