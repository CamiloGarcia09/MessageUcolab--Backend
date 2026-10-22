package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.PersonalWorkspaceRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class PersonalWorkspaceSurrealAdapter extends SurrealCatalogSupport implements PersonalWorkspaceRepository {
    private final CatalogPort catalog;

    public PersonalWorkspaceSurrealAdapter(Surreal surreal, LoggingPortFactory loggerFactory, CatalogPort catalog) {
        super(surreal, loggerFactory.getLogger(PersonalWorkspaceSurrealAdapter.class));
        this.catalog = catalog;
    }

    @Override
    public void saveUser(UUID userId, SessionIdentity identity) {
        if (userMatches(userId, identity)) {
            return;
        }
        try {
            execute("UPSERT " + userRecord(userId) + " SET identity_source = " + quote(identity.source())
                    + ", tenant_id = " + quote(identity.tenantId()) + ", subject = " + quote(identity.subject())
                    + ", email = " + quote(identity.email()) + ", name = " + quote(identity.name()) + ";");
        } catch (BusinessException exception) {
            // Two concurrent first logins may create the same deterministic user.
            // Accept only an independently confirmed, identical persisted identity.
            if (!userMatches(userId, identity)) {
                throw exception;
            }
        }
    }

    private boolean userMatches(UUID userId, SessionIdentity identity) {
        try {
            return queryOne("SELECT * FROM " + userRecord(userId) + ";",
                    catalog.getMessage(MessageCatalogCodeEnum.TCH_077.getCode()),
                    document -> identity.source().equals(stringOf(document.get("identity_source")))
                            && identity.tenantId().equals(stringOf(document.get("tenant_id")))
                            && identity.subject().equals(stringOf(document.get("subject")))
                            && identity.email().equals(stringOf(document.get("email")))
                            && identity.name().equals(stringOf(document.get("name")))).orElse(false);
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<CatalogItemDTO> findOrganizations(UUID userId) {
        return items("SELECT id, name FROM organization WHERE owner_user_id = " + userRecord(userId)
                + " ORDER BY name, id;");
    }

    @Override
    public List<CatalogItemDTO> findApplications(UUID userId) {
        return items("SELECT id, name FROM application WHERE organization_id.owner_user_id = " + userRecord(userId)
                + " ORDER BY name, id;");
    }

    @Override
    public List<CatalogItemDTO> findApplicationsByOrganization(UUID userId, UUID organizationId) {
        return items("SELECT id, name FROM application WHERE organization_id = "
                + recordIdLiteral("organization", organizationId.toString())
                + " AND organization_id.owner_user_id = " + userRecord(userId) + " ORDER BY name, id;");
    }

    @Override
    public boolean ownsOrganization(UUID userId, UUID organizationId) {
        return !items("SELECT id, name FROM " + recordIdLiteral("organization", organizationId.toString())
                + " WHERE owner_user_id = " + userRecord(userId) + ";").isEmpty();
    }

    @Override
    public boolean organizationNameExists(UUID userId, String name) {
        return !items("SELECT id, name FROM organization WHERE owner_user_id = " + userRecord(userId)
                + " AND name = " + quote(name) + " LIMIT 1;").isEmpty();
    }

    @Override
    public void createOrganization(OrganizationEntity organization, UUID userId) {
        execute("CREATE " + recordIdLiteral("organization", organization.getId().toString())
                + " CONTENT { name: " + quote(organization.getName()) + ", owner_user_id: " + userRecord(userId) + " };");
    }

    private List<CatalogItemDTO> items(String sql) {
        try {
            return query(sql, catalog.getMessage(MessageCatalogCodeEnum.TCH_077.getCode()), this::item);
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    private CatalogItemDTO item(Object document) {
        return CatalogItemDTO.create(extractIdAsUUID(document.get("id")).toString(), stringOf(document.get("name")));
    }

    private void execute(String sql) {
        try {
            surreal.query(sql);
        } catch (RuntimeException exception) {
            throw failure(exception);
        }
    }

    private BusinessException failure(RuntimeException cause) {
        return BusinessException.buildTechnicalException(catalog.getMessage(MessageCatalogCodeEnum.TCH_077.getCode()),
                cause, ExceptionLocation.INFRASTRUCTURE);
    }

    private static String userRecord(UUID id) {
        return recordIdLiteral("app_user", id.toString());
    }
}
