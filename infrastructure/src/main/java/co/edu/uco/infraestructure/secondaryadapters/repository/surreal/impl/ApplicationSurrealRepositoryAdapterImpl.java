package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class ApplicationSurrealRepositoryAdapterImpl extends SurrealCatalogSupport implements ApplicationRepository {

    private static final String SURREAL_TABLE_APPLICATION = "application";
    private static final String SURREAL_TABLE_ORGANIZATION = "organization";
    private static final String SURREAL_TABLE_LANGUAGE_BASE = "language_base";
    private static final String SURREAL_TABLE_APPLICATION_STATE = "application_state";
    private static final String SURREAL_TABLE_ENVIRONMENT = "environment";
    private static final String SURREAL_TABLE_ENVIRONMENT_TYPE = "environment_type";
    private static final String SURREAL_TABLE_ENVIRONMENT_STATE = "environment_state";
    private static final String SELECT_ALL_FROM = "SELECT * FROM ";
    private static final String LIMIT_ONE = " LIMIT 1;";

    public ApplicationSurrealRepositoryAdapterImpl(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(ApplicationSurrealRepositoryAdapterImpl.class));
    }

    @Override
    public Optional<ApplicationData> findByName(final String name) {
        final String sql = SELECT_ALL_FROM + SURREAL_TABLE_APPLICATION
                + " WHERE name = " + quote(name) + LIMIT_ONE;
        return queryOne(sql, "Error al consultar aplicación por nombre en SurrealDB: " + sql, this::toApplicationData);
    }

    @Override
    public boolean existsByNameAndOrganization(final String name, final String organizationId) {
        final String sql = SELECT_ALL_FROM + SURREAL_TABLE_APPLICATION + " WHERE name = " + quote(name)
                + " AND organization_id = " + recordIdLiteral(SURREAL_TABLE_ORGANIZATION, organizationId) + LIMIT_ONE;
        return queryOne(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_077.getCode()),
                this::toApplicationData).isPresent();
    }

    @Override
    public Optional<ApplicationData> findById(final String id) {
        final String sql = "SELECT *, organization_id.name AS organization_name FROM "
                + recordIdLiteral(SURREAL_TABLE_APPLICATION, id) + LIMIT_ONE;
        return queryOne(sql, "Error al consultar aplicación por id en SurrealDB: " + sql, this::toApplicationData);
    }

    @Override
    public boolean existsById(final String id) {
        final String sql = SELECT_ALL_FROM + recordIdLiteral(SURREAL_TABLE_APPLICATION, id) + LIMIT_ONE;
        return queryOne(sql, "Error al validar aplicación en SurrealDB: " + sql, obj -> obj).isPresent();
    }

    @Override
    public void createWithEnvironments(final ApplicationData application, final String languageId, final String stateId,
                                       final List<EnvironmentData> environments,
                                       final String environmentStateId) {
        final String applicationUpsert = "CREATE "
                + recordIdLiteral(SURREAL_TABLE_APPLICATION, application.getId().toString())
                + " CONTENT { "
                + "name: " + quote(application.getName()) + ", "
                + "organization_id: " + recordIdLiteral(
                        SURREAL_TABLE_ORGANIZATION, application.getOrganization().getId().toString()) + ", "
                + "language_id: " + recordIdLiteral(SURREAL_TABLE_LANGUAGE_BASE, languageId) + ", "
                + "state_id: " + recordIdLiteral(SURREAL_TABLE_APPLICATION_STATE, stateId)
                + " };";
        final StringBuilder transaction = new StringBuilder("BEGIN TRANSACTION;")
                .append(applicationUpsert);
        environments.forEach(environment -> transaction.append(environmentUpsert(environment, environmentStateId)));
        transaction.append("COMMIT TRANSACTION;");

        try {
            log.info("Creating application and {} default environments atomically", environments.size());
            surreal.query(transaction.toString());
        } catch (Exception ex) {
            log.error("Error al persistir la aplicación y sus ambientes en SurrealDB", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al persistir la aplicación y sus ambientes en la base de datos SurrealDB",
                    ex, ExceptionLocation.INFRASTRUCTURE);
        }
    }

    private String environmentUpsert(final EnvironmentData environment, final String stateId) {
        return "UPSERT " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT, environment.getId().toString())
                + " CONTENT { "
                + "application_id: " + recordIdLiteral(
                        SURREAL_TABLE_APPLICATION, environment.getApplication().getId().toString()) + ", "
                + "type_id: " + recordIdLiteral(
                        SURREAL_TABLE_ENVIRONMENT_TYPE, environment.getType().getId().toString()) + ", "
                + "state_id: " + recordIdLiteral(SURREAL_TABLE_ENVIRONMENT_STATE, stateId)
                + " };";
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
