package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class FunctionalityCatalogSurrealAdapter extends SurrealCatalogSupport implements FunctionalityCatalogRepository {

    private static final String SURREAL_TABLE_FUNCTIONALITY = "functionality";

    public FunctionalityCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(FunctionalityCatalogSurrealAdapter.class));
    }

    @Override
    public List<FunctionalityData> findAllByApplicationId(final String applicationId) {
        final String appRecordId = "application:" + applicationId;
        final String sql = "SELECT *, state_id.name AS state_name FROM " + SURREAL_TABLE_FUNCTIONALITY
                + " WHERE application_id = " + quote(appRecordId)
                + " OR application_id = " + recordIdLiteral("application", applicationId) + ";";
        return query(sql, "Error al consultar funcionalidades en SurrealDB: " + sql, this::toFunctionalityData);
    }

    private FunctionalityData toFunctionalityData(final Object obj) {
        final FunctionalityData data = FunctionalityData.build();
        data.setId(extractIdAsUUID(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        data.setStateId(extractIdAsUUID(obj.get("state_id")));
        data.setState(stringOf(obj.get("state_name")));

        final ApplicationData app = ApplicationData.build();
        final Value appIdValue = obj.get("application_id");
        if (!isNullObject(appIdValue)) {
            app.setId(extractIdAsUUID(appIdValue));
        }
        data.setApplication(app);
        return data;
    }
}
