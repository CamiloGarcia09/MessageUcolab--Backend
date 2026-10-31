package co.edu.uco.application.usecase.domain.aggregate.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionalityEntityTest {

    @Test
    void setId_usesDefaultUUIDWhenNull() {
        FunctionalityEntity entity = new FunctionalityEntity();

        entity.setId(null);

        assertThat(entity.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setId_acceptsValidUUID() {
        FunctionalityEntity entity = new FunctionalityEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    void setName_trimsValue() {
        FunctionalityEntity entity = new FunctionalityEntity();

        entity.setName("  functionality name  ");

        assertThat(entity.getName()).isEqualTo("functionality name");
    }

    @Test
    void setName_acceptsValidName() {
        FunctionalityEntity entity = new FunctionalityEntity();

        entity.setName("auth-service");

        assertThat(entity.getName()).isEqualTo("auth-service");
    }
}
