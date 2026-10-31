package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.infraestructure.secondaryadapters.repository.data.OrganizationSurrealMapper;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.OrganizationSurrealModel;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class OrganizationSurrealRepositoryAdapterImpl extends SurrealCatalogSupport
        implements OrganizationRepository {

    private static final String SURREAL_TABLE_ORGANIZATION = "organization";
    private static final String FIND_ERROR_MESSAGE = "Error al consultar organizacion en SurrealDB";
    private final OrganizationSurrealMapper mapper;

    public OrganizationSurrealRepositoryAdapterImpl(final Surreal surreal, final LoggingPortFactory loggerFactory,
                                                     final OrganizationSurrealMapper mapper) {
        super(surreal, loggerFactory.getLogger(OrganizationSurrealRepositoryAdapterImpl.class));
        this.mapper = mapper;
    }

    @Override
    public Optional<OrganizationEntity> findById(final UUID id) {
        final String sql = "SELECT * FROM "
                + recordIdLiteral(SURREAL_TABLE_ORGANIZATION, id.toString()) + " LIMIT 1;";
        return findOne(sql);
    }

    @Override
    public Optional<OrganizationEntity> findByName(final String name) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_ORGANIZATION
                + " WHERE name = " + quote(name) + " LIMIT 1;";
        return findOne(sql);
    }

    @Override
    public void create(final OrganizationEntity organization) {
        final OrganizationSurrealModel model = mapper.mapperModel(organization);
        final String sql = "UPSERT " + recordIdLiteral(SURREAL_TABLE_ORGANIZATION, model.getId().toString())
                + " CONTENT { name: " + quote(model.getName()) + " };";
        try {
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_103.getCode()));
            surreal.query(sql);
        } catch (final Exception ex) {
            log.error("Error al persistir la organizacion en SurrealDB", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al persistir la organizacion en la base de datos SurrealDB",
                    ex,
                    ExceptionLocation.INFRASTRUCTURE);
        }
    }

    private Optional<OrganizationEntity> findOne(final String sql) {
        return queryOne(sql, FIND_ERROR_MESSAGE, this::toModel).map(mapper::mapperData);
    }

    private OrganizationSurrealModel toModel(final Object document) {
        return new OrganizationSurrealModel(
                extractIdAsUUID(document.get("id")),
                stringOf(document.get("name")));
    }
}
