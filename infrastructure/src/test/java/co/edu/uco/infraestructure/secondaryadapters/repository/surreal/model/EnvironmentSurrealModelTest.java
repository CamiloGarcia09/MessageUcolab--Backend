package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSurrealModelTest {

    @Test
    void defaultConstructor_initializesEmptyFields() {
        EnvironmentSurrealModel model = new EnvironmentSurrealModel();

        assertThat(model.getId()).isNotNull();
        assertThat(model.getApplicationId()).isEmpty();
        assertThat(model.getTypeId()).isEmpty();
        assertThat(model.getStateId()).isEmpty();
        assertThat(model.getCreatedAt()).isNotNull();
        assertThat(model.getUpdatedAt()).isNotNull();
    }

    @Test
    void fullConstructor_assignsValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 10, 0);
        EnvironmentSurrealModel model = new EnvironmentSurrealModel(id, "app-1", "type-1", "st-1",
                createdAt, updatedAt);

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getApplicationId()).isEqualTo("app-1");
        assertThat(model.getTypeId()).isEqualTo("type-1");
        assertThat(model.getStateId()).isEqualTo("st-1");
        assertThat(model.getCreatedAt()).isEqualTo(createdAt);
        assertThat(model.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void setters_trimValuesAndApplyDefaults() {
        EnvironmentSurrealModel model = EnvironmentSurrealModel.build();
        model.setApplicationId("  app-1  ");
        model.setTypeId("  type-1  ");
        model.setStateId("  st-1  ");
        model.setId(null);
        model.setUpdatedAt(null);

        assertThat(model.getApplicationId()).isEqualTo("app-1");
        assertThat(model.getTypeId()).isEqualTo("type-1");
        assertThat(model.getStateId()).isEqualTo("st-1");
        assertThat(model.getId()).isNotNull();
        assertThat(model.getUpdatedAt()).isNotNull();
    }
}
