package co.edu.uco.application.secondaryports.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTypeDataTest {

    @Test
    void defaultConstructor_setsDefaults() {
        EnvironmentTypeData type = new EnvironmentTypeData();

        assertThat(type.getId()).isNotNull();
        assertThat(type.getName()).isEmpty();
    }

    @Test
    void parameterizedConstructor_storesValues() {
        UUID id = UUID.randomUUID();
        EnvironmentTypeData type = new EnvironmentTypeData(id, "dev");

        assertThat(type.getId()).isEqualTo(id);
        assertThat(type.getName()).isEqualTo("dev");
    }

    @Test
    void setName_trimsValue() {
        EnvironmentTypeData type = new EnvironmentTypeData();

        type.setName("  dev  ");

        assertThat(type.getName()).isEqualTo("dev");
    }

    @Test
    void setId_usesDefaultWhenNull() {
        EnvironmentTypeData type = new EnvironmentTypeData();

        type.setId(null);

        assertThat(type.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void build_returnsDefaultInstance() {
        EnvironmentTypeData type = EnvironmentTypeData.build();

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEmpty();
    }
}