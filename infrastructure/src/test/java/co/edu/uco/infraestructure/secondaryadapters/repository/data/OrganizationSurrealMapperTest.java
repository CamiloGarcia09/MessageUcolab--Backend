package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.OrganizationSurrealModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationSurrealMapperTest {

    private final OrganizationSurrealMapper mapper = new OrganizationSurrealMapper();

    @Test
    void mapperData_mapsModelToEntity() {
        UUID id = UUID.randomUUID();
        OrganizationSurrealModel model = new OrganizationSurrealModel(id, "UCO");

        OrganizationEntity organization = mapper.mapperData(model);

        assertThat(organization)
                .extracting(OrganizationEntity::getId, OrganizationEntity::getName)
                .containsExactly(id, "UCO");
    }

    @Test
    void mapperModel_mapsEntityToModel() {
        UUID id = UUID.randomUUID();
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(id);
        organization.setName("UCO");

        OrganizationSurrealModel model = mapper.mapperModel(organization);

        assertThat(model)
                .extracting(OrganizationSurrealModel::getId, OrganizationSurrealModel::getName)
                .containsExactly(id, "UCO");
    }
}
