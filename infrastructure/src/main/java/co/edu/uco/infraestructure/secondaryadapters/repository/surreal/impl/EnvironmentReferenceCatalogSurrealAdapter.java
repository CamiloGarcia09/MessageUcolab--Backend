package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;

@Repository
public class EnvironmentReferenceCatalogSurrealAdapter extends SurrealCatalogSupport
        implements EnvironmentReferenceCatalogRepository {

    private static final String ENVIRONMENT_TYPE_TABLE = "environment_type";
    private static final String ENVIRONMENT_STATE_TABLE = "environment_state";

    public EnvironmentReferenceCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(EnvironmentReferenceCatalogSurrealAdapter.class));
    }

    @Override
    public List<EnvironmentTypeData> findAllTypes() {
        return queryAll(ENVIRONMENT_TYPE_TABLE, "Error al consultar tipos de ambiente en SurrealDB: ",
                object -> new EnvironmentTypeData(extractCatalogId(object.get("id")), stringOf(object.get("name"))));
    }

    @Override
    public Optional<UUID> findStateIdByName(final String name) {
        final String sql = "SELECT * FROM " + ENVIRONMENT_STATE_TABLE
                + " WHERE string::lowercase(name) = string::lowercase(" + quote(name) + ") LIMIT 1;";
        return queryOne(sql, "Error al consultar estado de ambiente en SurrealDB: " + sql,
                object -> extractCatalogId(object.get("id")));
    }
}
