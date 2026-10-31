package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationSurrealModelTest {

    @Test
    void defaultConstructor_initializesDefaultValues() {
        OrganizationSurrealModel model = new OrganizationSurrealModel();

        assertThat(model.getId()).isNotNull();
        assertThat(model.getName()).isEmpty();
    }

    @Test
    void fullConstructor_assignsNormalizedValues() {
        UUID id = UUID.randomUUID();

        OrganizationSurrealModel model = new OrganizationSurrealModel(id, "  UCO  ");

        assertThat(model)
                .extracting(OrganizationSurrealModel::getId, OrganizationSurrealModel::getName)
                .containsExactly(id, "UCO");
    }

    @Test
    void setters_applyDefaults_whenValuesAreMissing() {
        OrganizationSurrealModel model = OrganizationSurrealModel.build();

        model.setId(null);
        model.setName(null);

        assertThat(model)
                .extracting(OrganizationSurrealModel::getId, OrganizationSurrealModel::getName)
                .containsExactly(UUID.fromString("00000000-0000-0000-0000-000000000000"), "");
    }
}
