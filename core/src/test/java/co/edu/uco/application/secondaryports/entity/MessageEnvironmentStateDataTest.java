package co.edu.uco.application.secondaryports.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEnvironmentStateDataTest {

    @Test
    void defaultConstructor_setsDefaults() {
        MessageEnvironmentStateData data = new MessageEnvironmentStateData();

        assertThat(data.getId()).isNotNull();
        assertThat(data.getName()).isEmpty();
    }

    @Test
    void parameterizedConstructor_storesValues() {
        UUID id = UUID.randomUUID();
        MessageEnvironmentStateData data = new MessageEnvironmentStateData(id, " state ");

        assertThat(data.getId()).isEqualTo(id);
        assertThat(data.getName()).isEqualTo("state");
    }

    @Test
    void setId_usesDefaultWhenNull() {
        MessageEnvironmentStateData data = new MessageEnvironmentStateData();

        data.setId(null);

        assertThat(data.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void build_returnsDefaultInstance() {
        MessageEnvironmentStateData data = MessageEnvironmentStateData.build();

        assertThat(data).isNotNull();
        assertThat(data.getName()).isEmpty();
    }
}