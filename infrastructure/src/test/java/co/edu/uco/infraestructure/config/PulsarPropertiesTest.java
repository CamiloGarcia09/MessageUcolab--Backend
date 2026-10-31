package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PulsarPropertiesTest {

    @Test
    void gettersAndSetters_roundTripValues() {
        PulsarProperties properties = new PulsarProperties();
        properties.setServiceUrl("pulsar://localhost:6650");
        properties.setTopicName("messages");

        assertThat(properties.getServiceUrl()).isEqualTo("pulsar://localhost:6650");
        assertThat(properties.getTopicName()).isEqualTo("messages");
    }
}