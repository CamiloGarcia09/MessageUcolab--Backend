package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class EnvironmentCatalogSurrealAdapter extends SurrealCatalogSupport implements EnvironmentCatalogRepository {

    private static final String SURREAL_TABLE_ENVIRONMENT = "environment";

    public EnvironmentCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(EnvironmentCatalogSurrealAdapter.class));
    }

    @Override
    public List<EnvironmentData> findAllByApplicationId(final String applicationId) {
        final String appRecordId = "application:" + applicationId;
        final String sql = "SELECT *, type_id.name AS type_name FROM " + SURREAL_TABLE_ENVIRONMENT
                + " WHERE application_id = " + quote(appRecordId)
                + " OR application_id = " + recordIdLiteral("application", applicationId) + ";";
        return query(sql, "Error al consultar entornos en SurrealDB: " + sql, this::toEnvironmentData);
    }

    private EnvironmentData toEnvironmentData(final Object obj) {
        final EnvironmentData data = EnvironmentData.build();
        data.setId(extractIdAsUUID(obj.get("id")));
        final ApplicationData app = ApplicationData.build();
        final Value appIdValue = obj.get("application_id");
        if (!isNullObject(appIdValue)) {
            app.setId(extractIdAsUUID(appIdValue));
        }
        data.setApplication(app);

        final Value typeIdValue = obj.get("type_id");
        if (!isNullObject(typeIdValue)) {
            data.setType(new EnvironmentTypeData(
                    extractIdAsUUID(typeIdValue), stringOf(obj.get("type_name"))));
        }
        return data;
    }
}
