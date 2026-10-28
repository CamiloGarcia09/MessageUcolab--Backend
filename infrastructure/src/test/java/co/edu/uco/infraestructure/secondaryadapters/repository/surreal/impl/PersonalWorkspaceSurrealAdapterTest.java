package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.application.primaryports.dto.catalog.ApplicationCatalogItemDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_077;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalWorkspaceSurrealAdapter")
class PersonalWorkspaceSurrealAdapterTest {
    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ORGANIZATION_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    private static final String USER_TABLE = "app_user:`" + USER_ID + "`";

    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort logger;
    @Mock private CatalogPort catalog;

    private PersonalWorkspaceSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(PersonalWorkspaceSurrealAdapter.class)).thenReturn(logger);
        adapter = new PersonalWorkspaceSurrealAdapter(surreal, loggerFactory, catalog);
    }

    @Test
    @DisplayName("filters organizations and applications by their authenticated owner")
    void lookupMethods_useOwnerScopedSurrealQueries() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.findOrganizations(USER_ID)).isEmpty();
        assertThat(adapter.findApplications(USER_ID)).isEmpty();
        assertThat(adapter.findApplicationsByOrganization(USER_ID, ORGANIZATION_ID)).isEmpty();
        assertThat(adapter.ownsOrganization(USER_ID, ORGANIZATION_ID)).isFalse();

        verify(surreal).query("SELECT id, name FROM organization WHERE owner_user_id = " + USER_TABLE + " ORDER BY name, id;");
        verify(surreal).query("SELECT id, name, language_id, language_id.language AS language_name FROM application WHERE organization_id.owner_user_id = "
                + USER_TABLE + " ORDER BY name, id;");
        verify(surreal).query("SELECT id, name, language_id, language_id.language AS language_name FROM application WHERE organization_id = organization:`"
                + ORGANIZATION_ID + "` AND organization_id.owner_user_id = " + USER_TABLE + " ORDER BY name, id;");
        verify(surreal).query("SELECT id, name FROM organization:`" + ORGANIZATION_ID
                + "` WHERE owner_user_id = " + USER_TABLE + ";");
    }

    @Test
    @DisplayName("maps selected language identifiers and names from SDK rows; missing names yield no language")
    void findApplications_mapsLanguageFromSurrealResponse() {
        var english = applicationDocument("123e4567-e89b-12d3-a456-426614174020", "English",
                "123e4567-e89b-12d3-a456-426614174021");
        var spanish = applicationDocument("123e4567-e89b-12d3-a456-426614174022", "Español",
                "123e4567-e89b-12d3-a456-426614174023");
        var blankName = applicationDocument("123e4567-e89b-12d3-a456-426614174024", "", null);
        var missingName = applicationDocument("123e4567-e89b-12d3-a456-426614174026", null, null);
        doReturn(responseWithOne(english), responseWithOne(spanish), responseWithOne(blankName),
                responseWithOne(missingName)).when(surreal).query(anyString());

        var englishItems = adapter.findApplications(USER_ID);
        var spanishItems = adapter.findApplications(USER_ID);
        var blankNameItems = adapter.findApplications(USER_ID);
        var missingNameItems = adapter.findApplications(USER_ID);

        assertThat(((ApplicationCatalogItemDTO) englishItems.getFirst()).getLanguage())
                .isEqualTo(CatalogItemDTO.create("123e4567-e89b-12d3-a456-426614174021", "English"));
        assertThat(((ApplicationCatalogItemDTO) spanishItems.getFirst()).getLanguage())
                .isEqualTo(CatalogItemDTO.create("123e4567-e89b-12d3-a456-426614174023", "Español"));
        assertThat(((ApplicationCatalogItemDTO) blankNameItems.getFirst()).getLanguage()).isNull();
        assertThat(((ApplicationCatalogItemDTO) missingNameItems.getFirst()).getLanguage()).isNull();
        verify(surreal, times(4)).query("SELECT id, name, language_id, language_id.language AS language_name FROM application WHERE organization_id.owner_user_id = "
                + USER_TABLE + " ORDER BY name, id;");
    }

    @Test
    @DisplayName("escapes organization names and binds duplicate-name checks to the owner")
    void organizationNameExists_escapesNameWithinOwnerScope() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.organizationNameExists(USER_ID, "Owner's Org")).isFalse();

        verify(surreal).query("SELECT id, name FROM organization WHERE owner_user_id = " + USER_TABLE
                + " AND name = 'Owner\\'s Org' LIMIT 1;");
    }

    @Test
    @DisplayName("creates organization with authenticated owner reference")
    void createOrganization_persistsServerIdAndOwner() {
        OrganizationEntity organization = organization(ORGANIZATION_ID, "Personal Org");

        adapter.createOrganization(organization, USER_ID);

        verify(surreal).query("CREATE organization:`" + ORGANIZATION_ID + "` CONTENT { name: 'Personal Org', owner_user_id: "
                + USER_TABLE + " };");
    }

    @Test
    @DisplayName("creates local identity reference when deterministic user record is absent")
    void saveUser_missingUser_createsIdentityReference() {
        SessionIdentity identity = new SessionIdentity("issuer", "tenant", "subject", "mail@example.test", "Name");
        doReturn(emptyResponse()).when(surreal).query("SELECT * FROM " + USER_TABLE + ";");

        adapter.saveUser(USER_ID, identity);

        verify(surreal).query("UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                + "subject = 'subject', email = 'mail@example.test', name = 'Name';");
    }

    @Test
    @DisplayName("avoids rewriting unchanged user identity but persists mutable profile updates")
    void saveUser_userExists_skipsOrUpdatesAccordingToIdentityFields() {
        SessionIdentity oldProfile = new SessionIdentity("issuer", "tenant", "subject", "old@example.test", "Name");
        SessionIdentity changedProfile = new SessionIdentity("issuer", "tenant", "subject", "new@example.test", "Name");
        doReturn(responseWithOne(userDocument(oldProfile)), responseWithOne(userDocument(oldProfile)))
                .when(surreal).query("SELECT * FROM " + USER_TABLE + ";");

        adapter.saveUser(USER_ID, oldProfile);
        adapter.saveUser(USER_ID, changedProfile);

        verify(surreal, never()).query("UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                + "subject = 'subject', email = 'old@example.test', name = 'Name';");
        verify(surreal).query("UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                + "subject = 'subject', email = 'new@example.test', name = 'Name';");
    }

    @Test
    @DisplayName("accepts a concurrent duplicate create only after confirming the same persisted identity")
    void saveUser_racingCreate_confirmsIdenticalIdentityBeforeSuppressingFailure() {
        SessionIdentity identity = new SessionIdentity("issuer", "tenant", "subject", "mail@example.test", "Name");
        when(catalog.getMessage(TCH_077.getCode())).thenReturn("Workspace persistence failed");
        doReturn(emptyResponse(), responseWithOne(userDocument(identity)))
                .when(surreal).query("SELECT * FROM " + USER_TABLE + ";");
        doThrow(new IllegalStateException("duplicate record"))
                .when(surreal).query("UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                        + "subject = 'subject', email = 'mail@example.test', name = 'Name';");

        adapter.saveUser(USER_ID, identity);

        verify(surreal).query("UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                + "subject = 'subject', email = 'mail@example.test', name = 'Name';");
        verify(surreal, times(2)).query("SELECT * FROM " + USER_TABLE + ";");
    }

    @Test
    @DisplayName("does not hide a failed write when the concurrent identity does not match")
    void saveUser_racingCreateWithDifferentIdentity_throwsTechnicalDomainFailure() {
        SessionIdentity identity = new SessionIdentity("issuer", "tenant", "subject", "mail@example.test", "Name");
        SessionIdentity otherIdentity = new SessionIdentity("issuer", "other-tenant", "other-subject", "mail@example.test", "Name");
        when(catalog.getMessage(TCH_077.getCode())).thenReturn("Workspace persistence failed");
        doReturn(emptyResponse(), responseWithOne(userDocument(otherIdentity)))
                .when(surreal).query("SELECT * FROM " + USER_TABLE + ";");
        doThrow(new IllegalStateException("write rejected")).when(surreal).query(
                "UPSERT " + USER_TABLE + " SET identity_source = 'issuer', tenant_id = 'tenant', "
                        + "subject = 'subject', email = 'mail@example.test', name = 'Name';");

        assertThatThrownBy(() -> adapter.saveUser(USER_ID, identity)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("wraps query failures with the workspace technical domain exception")
    void findOrganizations_queryFails_throwsBusinessException() {
        when(catalog.getMessage(TCH_077.getCode())).thenReturn("Workspace persistence failed");
        doThrow(new IllegalStateException("database unavailable")).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.findOrganizations(USER_ID)).isInstanceOf(BusinessException.class);
    }

    private static OrganizationEntity organization(UUID id, String name) {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private Object userDocument(SessionIdentity identity) {
        Object document = org.mockito.Mockito.mock(Object.class);
        Value source = stringValue(identity.source());
        Value tenant = stringValue(identity.tenantId());
        Value subject = stringValue(identity.subject());
        Value email = stringValue(identity.email());
        Value name = stringValue(identity.name());
        lenient().when(document.get("identity_source")).thenReturn(source);
        lenient().when(document.get("tenant_id")).thenReturn(tenant);
        lenient().when(document.get("subject")).thenReturn(subject);
        lenient().when(document.get("email")).thenReturn(email);
        lenient().when(document.get("name")).thenReturn(name);
        return document;
    }

    private Value stringValue(String string) {
        Value value = org.mockito.Mockito.mock(Value.class);
        lenient().when(value.isNull()).thenReturn(false);
        lenient().when(value.isNone()).thenReturn(false);
        lenient().when(value.isString()).thenReturn(true);
        lenient().when(value.getString()).thenReturn(string);
        return value;
    }

    private Response responseWithOne(Object document) {
        Response response = org.mockito.Mockito.mock(Response.class);
        Value statement = org.mockito.Mockito.mock(Value.class);
        Array array = org.mockito.Mockito.mock(Array.class);
        Value item = org.mockito.Mockito.mock(Value.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(array);
        when(array.len()).thenReturn(1);
        when(array.get(0)).thenReturn(item);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(document);
        return response;
    }

    private Response emptyResponse() {
        Response response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    private Object applicationDocument(String applicationId, String languageName, String languageId) {
        Object document = org.mockito.Mockito.mock(Object.class);
        doReturn(stringValue("application:" + applicationId)).when(document).get("id");
        doReturn(stringValue("Application " + applicationId)).when(document).get("name");
        if (languageName != null) {
            doReturn(stringValue(languageName)).when(document).get("language_name");
        } else {
            Value none = org.mockito.Mockito.mock(Value.class);
            when(none.isNull()).thenReturn(false);
            when(none.isNone()).thenReturn(true);
            doReturn(none).when(document).get("language_name");
        }
        if (languageId != null) {
            doReturn(stringValue("language_base:" + languageId)).when(document).get("language_id");
        }
        return document;
    }

}
