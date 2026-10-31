package co.edu.uco.application.secondaryports.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatusMessageDataTest {

    @Test
    void defaultConstructor_setsDefaults() {
        StatusMessageData data = new StatusMessageData();

        assertThat(data.getId()).isNotNull();
        assertThat(data.getName()).isEmpty();
    }

    @Test
    void parameterizedConstructor_storesValues() {
        UUID id = UUID.randomUUID();

        StatusMessageData data = new StatusMessageData(id, "  active  ");

        assertThat(data.getId()).isEqualTo(id);
        assertThat(data.getName()).isEqualTo("active");
    }

    @Test
    void setId_usesDefaultUUIDWhenNull() {
        StatusMessageData data = new StatusMessageData();

        data.setId(null);

        assertThat(data.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setName_trimsValue() {
        StatusMessageData data = new StatusMessageData();

        data.setName("  active  ");

        assertThat(data.getName()).isEqualTo("active");
    }

    @Test
    void build_returnsDefaultInstance() {
        StatusMessageData data = StatusMessageData.build();

        assertThat(data).isNotNull();
        assertThat(data.getId()).isNotNull();
        assertThat(data.getName()).isEmpty();
    }
}
