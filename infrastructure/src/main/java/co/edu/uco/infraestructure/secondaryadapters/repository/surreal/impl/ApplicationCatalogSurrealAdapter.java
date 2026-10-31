package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ApplicationCatalogSurrealAdapter extends SurrealCatalogSupport implements ApplicationCatalogRepository {

    private static final String FIND_ALL_APPLICATIONS =
            "SELECT *, organization_id.name AS organization_name FROM application;";

    public ApplicationCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(ApplicationCatalogSurrealAdapter.class));
    }

    @Override
    public List<ApplicationData> findAll() {
        return query(FIND_ALL_APPLICATIONS, "Error al consultar aplicaciones en SurrealDB: "
                + FIND_ALL_APPLICATIONS, this::toApplicationData);
    }

    private ApplicationData toApplicationData(final Object obj) {
        final ApplicationData data = ApplicationData.build();
        data.setId(extractIdAsUUID(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        final OrganizationEntity organization = new OrganizationEntity();
        organization.setId(extractIdAsUUID(obj.get("organization_id")));
        organization.setName(stringOf(obj.get("organization_name")));
        data.setOrganization(organization);
        return data;
    }
}
