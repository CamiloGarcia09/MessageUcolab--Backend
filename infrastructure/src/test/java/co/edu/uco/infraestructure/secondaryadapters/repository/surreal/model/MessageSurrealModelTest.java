package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSurrealModelTest {

    @Test
    void defaultConstructor_initializesEmptyFields() {
        MessageSurrealModel model = new MessageSurrealModel();

        assertThat(model.getId()).isNotNull();
        assertThat(model.getCode()).isEmpty();
        assertThat(model.getTitle()).isEmpty();
        assertThat(model.getContent()).isEmpty();
        assertThat(model.getTypeId()).isEmpty();
        assertThat(model.getCategoryId()).isEmpty();
        assertThat(model.getStatusId()).isEmpty();
        assertThat(model.getApplicationId()).isEmpty();
        assertThat(model.getFunctionalityId()).isEmpty();
        assertThat(model.getCreatedAt()).isNotNull();
        assertThat(model.getUpdatedAt()).isNotNull();
    }

    @Test
    void fullConstructor_assignsValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 10, 0);
        MessageSurrealModel model = new MessageSurrealModel(id, "CODE", "Title", "Content", "type-1",
                "cat-1", "st-1", "APP", "func-1", createdAt, updatedAt);

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getCode()).isEqualTo("CODE");
        assertThat(model.getTitle()).isEqualTo("Title");
        assertThat(model.getContent()).isEqualTo("Content");
        assertThat(model.getTypeId()).isEqualTo("type-1");
        assertThat(model.getCategoryId()).isEqualTo("cat-1");
        assertThat(model.getStatusId()).isEqualTo("st-1");
        assertThat(model.getApplicationId()).isEqualTo("APP");
        assertThat(model.getFunctionalityId()).isEqualTo("func-1");
        assertThat(model.getCreatedAt()).isEqualTo(createdAt);
        assertThat(model.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void setters_trimValuesAndApplyDefaults() {
        MessageSurrealModel model = MessageSurrealModel.build();
        model.setCode("  code  ");
        model.setId(null);
        model.setCreatedAt(null);

        assertThat(model.getCode()).isEqualTo("code");
        assertThat(model.getId()).isNotNull();
        assertThat(model.getCreatedAt()).isNotNull();
    }
}