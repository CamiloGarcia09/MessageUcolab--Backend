package co.edu.uco.infraestructure.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackUtcConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"logback-spring.xml", "logback-azure.xml"})
    void logbackConfiguration_usesExplicitUtcTimestampFormat(String resourceName) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(resource).as("resource %s", resourceName).isNotNull();
            String content = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(content).contains("%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX,UTC}");
        }
    }
}
