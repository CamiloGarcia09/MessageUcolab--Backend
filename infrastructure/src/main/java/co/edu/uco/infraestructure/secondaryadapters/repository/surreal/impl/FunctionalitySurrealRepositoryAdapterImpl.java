package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class FunctionalitySurrealRepositoryAdapterImpl extends SurrealCatalogSupport implements FunctionalityRepository {

    private static final String SURREAL_TABLE_FUNCTIONALITY = "functionality";
    private static final String SURREAL_TABLE_APPLICATION = "application";
    private static final String SURREAL_TABLE_FUNCTIONALITY_STATE = "functionality_state";

    public FunctionalitySurrealRepositoryAdapterImpl(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(FunctionalitySurrealRepositoryAdapterImpl.class));
    }

    @Override
    public boolean existsByNameAndApplicationId(final String name, final String applicationId) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_FUNCTIONALITY
                + " WHERE name = " + quote(name)
                + " AND application_id = " + recordIdLiteral(SURREAL_TABLE_APPLICATION, applicationId)
                + " LIMIT 1;";
        return queryOne(sql, "Error al validar funcionalidad por nombre en SurrealDB: " + sql, obj -> obj).isPresent();
    }

    @Override
    public void create(final FunctionalityData functionality, final String stateId) {
        final String upsertSql = "UPSERT " + recordIdLiteral(SURREAL_TABLE_FUNCTIONALITY, functionality.getId().toString())
                + " CONTENT { "
                + "name: " + quote(functionality.getName()) + ", "
                + "application_id: " + recordIdLiteral(SURREAL_TABLE_APPLICATION, functionality.getApplication().getId().toString()) + ", "
                + "state_id: " + recordIdLiteral(SURREAL_TABLE_FUNCTIONALITY_STATE, stateId)
                + " };";
        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_102.getCode()), upsertSql);
            surreal.query(upsertSql);
        } catch (Exception ex) {
            log.error("Error al persistir la funcionalidad en SurrealDB", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al persistir la funcionalidad en la base de datos SurrealDB", ex, ExceptionLocation.INFRASTRUCTURE);
        }
    }
}
