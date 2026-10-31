package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SurrealDBPropertiesTest {

    @Test
    void gettersAndSetters_roundTripValues() {
        SurrealDBProperties properties = new SurrealDBProperties();
        properties.setHost("localhost");
        properties.setPort(8000);
        properties.setUsername("root");
        properties.setPassword("root");
        properties.setNamespace("ns");
        properties.setDatabase("db");
        properties.setMaxConnections(10);

        assertThat(properties.getHost()).isEqualTo("localhost");
        assertThat(properties.getPort()).isEqualTo(8000);
        assertThat(properties.getUsername()).isEqualTo("root");
        assertThat(properties.getPassword()).isEqualTo("root");
        assertThat(properties.getNamespace()).isEqualTo("ns");
        assertThat(properties.getDatabase()).isEqualTo("db");
        assertThat(properties.getMaxConnections()).isEqualTo(10);
    }
}