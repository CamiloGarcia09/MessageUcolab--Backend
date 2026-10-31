package co.edu.uco.application.usecase.domain.aggregate.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeEntityTest {

    @Test
    void setId_usesDefaultUUIDWhenNull() {
        MessageTypeEntity entity = new MessageTypeEntity();

        entity.setId(null);

        assertThat(entity.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setId_acceptsValidUUID() {
        MessageTypeEntity entity = new MessageTypeEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    void setName_trimsValue() {
        MessageTypeEntity entity = new MessageTypeEntity();

        entity.setName("  type name  ");

        assertThat(entity.getName()).isEqualTo("type name");
    }
}
