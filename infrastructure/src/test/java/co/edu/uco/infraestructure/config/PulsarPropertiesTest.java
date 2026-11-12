package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PulsarPropertiesTest {

    @Test
    void gettersAndSetters_roundTripValues() {
        PulsarProperties properties = new PulsarProperties();
        properties.setServiceUrl("pulsar://localhost:6650");
        properties.setTopicName("messages");
        properties.setEventsTopicPrefix("persistent://tenant/ns/events");
        properties.setEventsEnabled(false);

        assertThat(properties.getServiceUrl()).isEqualTo("pulsar://localhost:6650");
        assertThat(properties.getTopicName()).isEqualTo("messages");
        assertThat(properties.getEventsTopicPrefix()).isEqualTo("persistent://tenant/ns/events");
        assertThat(properties.isEventsEnabled()).isFalse();
    }

    @Test
    void eventsSettings_haveSafeDefaults() {
        PulsarProperties properties = new PulsarProperties();

        assertThat(properties.getEventsTopicPrefix()).isEqualTo("persistent://public/default/message-events");
        assertThat(properties.isEventsEnabled()).isTrue();
    }
}
