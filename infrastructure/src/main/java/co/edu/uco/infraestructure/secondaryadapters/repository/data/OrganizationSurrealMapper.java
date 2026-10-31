package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.OrganizationSurrealModel;
import org.springframework.stereotype.Component;

@Component
public final class OrganizationSurrealMapper
        implements DataMapper<OrganizationEntity, OrganizationSurrealModel> {

    @Override
    public OrganizationEntity mapperData(final OrganizationSurrealModel model) {
        final OrganizationEntity organization = new OrganizationEntity();
        organization.setId(model.getId());
        organization.setName(model.getName());
        return organization;
    }

    @Override
    public OrganizationSurrealModel mapperModel(final OrganizationEntity organization) {
        return new OrganizationSurrealModel(organization.getId(), organization.getName());
    }
}
