package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_109;
import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

/**
 * Relacion entre el client_id de una identidad de servicio y el ambiente que consume. El indice unico de
 * client_id garantiza que una identidad pertenezca a un solo ambiente.
 */
@Repository
public class ServiceIdentitySurrealAdapter extends SurrealCatalogSupport implements ServiceIdentityRepository {

    private static final String TABLE = "environment_service_identity";
    private static final String COLUMNS = "SELECT client_id, application_id, environment_id,"
            + " environment_id.type_id.name AS environment_name, created_at FROM " + TABLE;

    private final CatalogPort catalog;

    public ServiceIdentitySurrealAdapter(final Surreal surreal, final LoggingPortFactory logging,
                                         final CatalogPort catalog) {
        super(surreal, logging.getLogger(ServiceIdentitySurrealAdapter.class));
        this.catalog = catalog;
    }

    @Override
    public Optional<ServiceIdentityData> findByClientId(final String clientId) {
        String sql = COLUMNS + " WHERE client_id = " + quote(clientId) + " LIMIT 1;";
        try {
            return queryOne(sql, catalog.getMessage(TCH_109.getCode()), this::toData);
        } catch (final RuntimeException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<ServiceIdentityData> findByApplicationId(final String applicationId) {
        String sql = COLUMNS + " WHERE application_id = " + recordIdLiteral("application", applicationId)
                + " ORDER BY created_at;";
        try {
            return query(sql, catalog.getMessage(TCH_109.getCode()), this::toData);
        } catch (final RuntimeException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void register(final String clientId, final String applicationId, final String environmentId) {
        String sql = "CREATE " + TABLE + " CONTENT { client_id: " + quote(clientId)
                + ", application_id: " + recordIdLiteral("application", applicationId)
                + ", environment_id: " + recordIdLiteral("environment", environmentId) + " } RETURN NONE;";
        try {
            var response = surreal.query(sql);
            // El driver nativo puede diferir el error del indice unico hasta leer el resultado.
            response.take(0);
        } catch (final RuntimeException exception) {
            throw failure(exception);
        }
    }

    @Override
    public boolean remove(final String clientId, final String applicationId) {
        String sql = "DELETE " + TABLE + " WHERE client_id = " + quote(clientId)
                + " AND application_id = " + recordIdLiteral("application", applicationId) + " RETURN BEFORE;";
        try {
            return !query(sql, catalog.getMessage(TCH_109.getCode()), row -> row).isEmpty();
        } catch (final RuntimeException exception) {
            throw failure(exception);
        }
    }

    private ServiceIdentityData toData(final Object row) {
        return new ServiceIdentityData(
                stringOf(row.get("client_id")),
                extractIdAsUUID(row.get("application_id")).toString(),
                extractIdAsUUID(row.get("environment_id")).toString(),
                stringOf(row.get("environment_name")),
                dateOf(row.get("created_at")));
    }

    private static String dateOf(final Value value) {
        if (!isNullObject(value) && value.isDateTime()) {
            return value.getDateTime().toInstant().toString();
        }
        return stringOf(value);
    }

    private BusinessException failure(final RuntimeException exception) {
        if (exception instanceof BusinessException business) {
            return business;
        }
        String message = catalog.getMessage(TCH_109.getCode());
        log.error(message, exception);
        return BusinessException.buildTechnicalException(message, exception, ExceptionLocation.INFRASTRUCTURE);
    }
}
