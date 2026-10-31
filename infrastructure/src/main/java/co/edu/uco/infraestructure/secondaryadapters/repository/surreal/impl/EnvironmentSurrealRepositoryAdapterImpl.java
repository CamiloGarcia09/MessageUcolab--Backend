package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class EnvironmentSurrealRepositoryAdapterImpl extends SurrealCatalogSupport implements EnvironmentRepository {

    private static final String SURREAL_TABLE_ENVIRONMENT = "environment";
    private static final String SURREAL_TABLE_APPLICATION = "application";
    private static final String SURREAL_TABLE_ENVIRONMENT_TYPE = "environment_type";
    private static final String SURREAL_TABLE_ENVIRONMENT_STATE = "environment_state";

    public EnvironmentSurrealRepositoryAdapterImpl(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(EnvironmentSurrealRepositoryAdapterImpl.class));
    }

    @Override
    public boolean existsByApplicationIdAndTypeId(final String applicationId, final String typeId) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_ENVIRONMENT
                + " WHERE application_id = " + recordIdLiteral(SURREAL_TABLE_APPLICATION, applicationId)
                + " AND type_id = " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT_TYPE, typeId)
                + " LIMIT 1;";
        try {
            return queryOne(sql, "Error al validar entorno por aplicación y tipo en SurrealDB: " + sql,
                    obj -> obj).isPresent();
        } catch (final BusinessException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            throw technicalException("Error al validar el entorno en la base de datos SurrealDB", exception);
        }
    }

    @Override
    public void create(final EnvironmentData environment, final String typeId, final String stateId) {
        final String upsertSql = "UPSERT " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT, environment.getId().toString())
                + " CONTENT { "
                + "application_id: " + recordIdLiteral(SURREAL_TABLE_APPLICATION, environment.getApplication().getId().toString()) + ", "
                + "type_id: " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT_TYPE, typeId) + ", "
                + "state_id: " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT_STATE, stateId)
                + " };";
        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_101.getCode()), upsertSql);
            surreal.query(upsertSql);
        } catch (Exception ex) {
            log.error("Error al persistir el entorno en SurrealDB", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al persistir el entorno en la base de datos SurrealDB", ex, ExceptionLocation.INFRASTRUCTURE);
        }
    }

    @Override
    public Optional<EnvironmentData> findById(final String id) {
        final String sql = "SELECT *, type_id.name AS type_name FROM "
                + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT, id) + " LIMIT 1;";
        try {
            return queryOne(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_056.getCode()).formatted(sql),
                    this::toEnvironmentData);
        } catch (final BusinessException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            throw technicalException("Error al consultar el entorno en la base de datos SurrealDB", exception);
        }
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

    private BusinessException technicalException(final String message, final RuntimeException exception) {
        return BusinessException.buildTechnicalException(message, exception, ExceptionLocation.INFRASTRUCTURE);
    }
}
