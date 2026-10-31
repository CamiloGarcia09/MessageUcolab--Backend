package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationAiPropertiesTest {

    @Test
    void defaults_areApplied() {
        TranslationAiProperties properties = new TranslationAiProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getProvider()).isEqualTo("ollama");
        assertThat(properties.getBaseUrl()).isEqualTo("http://ollama:11434");
        assertThat(properties.getModelName()).isEqualTo("llama3.2");
        assertThat(properties.getTemperature()).isEqualTo(0.1);
        assertThat(properties.getMaxRetries()).isZero();
        assertThat(properties.getTimeoutSeconds()).isEqualTo(30);
    }

    @Test
    void gettersAndSetters_roundTripValues() {
        TranslationAiProperties properties = new TranslationAiProperties();
        properties.setEnabled(false);
        properties.setProvider("openai");
        properties.setApiKey("key");
        properties.setBaseUrl("http://localhost");
        properties.setModelName("gpt-4");
        properties.setTemperature(0.5);
        properties.setMaxRetries(3);
        properties.setTimeoutSeconds(60);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getProvider()).isEqualTo("openai");
        assertThat(properties.getApiKey()).isEqualTo("key");
        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost");
        assertThat(properties.getModelName()).isEqualTo("gpt-4");
        assertThat(properties.getTemperature()).isEqualTo(0.5);
        assertThat(properties.getMaxRetries()).isEqualTo(3);
        assertThat(properties.getTimeoutSeconds()).isEqualTo(60);
    }
}