package co.edu.uco.application.usecase.domain.aggregate.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCategoryEntityTest {

    @Test
    void setId_usesDefaultUUIDWhenNull() {
        MessageCategoryEntity entity = new MessageCategoryEntity();

        entity.setId(null);

        assertThat(entity.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setId_acceptsValidUUID() {
        MessageCategoryEntity entity = new MessageCategoryEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    void setName_trimsValue() {
        MessageCategoryEntity entity = new MessageCategoryEntity();

        entity.setName("  category name  ");

        assertThat(entity.getName()).isEqualTo("category name");
    }
}
