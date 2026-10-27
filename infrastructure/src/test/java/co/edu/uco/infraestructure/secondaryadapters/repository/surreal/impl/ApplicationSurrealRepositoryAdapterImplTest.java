package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationSurrealRepositoryAdapterImplTest {

    private static final String APPLICATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ORGANIZATION_ID = "223e4567-e89b-12d3-a456-426614174000";
    private static final String LANGUAGE_ID = "lang-1";
    private static final String STATE_ID = "state-1";
    private static final String ENVIRONMENT_STATE_ID = "523e4567-e89b-12d3-a456-426614174000";
    private static final String ENVIRONMENT_STATE_RECORD =
            "environment_state:`" + ENVIRONMENT_STATE_ID + "`";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private ApplicationSurrealRepositoryAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(ApplicationSurrealRepositoryAdapterImpl.class)).thenReturn(log);
        adapter = new ApplicationSurrealRepositoryAdapterImpl(surreal, loggerFactory);
    }

    private Value stringValue(String value) {
        Value v = mock(Value.class);
        when(v.isNull()).thenReturn(false);
        when(v.isNone()).thenReturn(false);
        when(v.isString()).thenReturn(true);
        when(v.getString()).thenReturn(value);
        return v;
    }

    private Value recordIdValue(String table, String uuid) {
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn(table + ":" + uuid);
        Value v = mock(Value.class);
        when(v.isRecordId()).thenReturn(true);
        when(v.getRecordId()).thenReturn(recordId);
        return v;
    }

    private Object applicationDocument(String uuid, String name, String organizationId) {
        Object doc = mock(Object.class);
        doReturn(recordIdValue("application", uuid)).when(doc).get("id");
        doReturn(stringValue(name)).when(doc).get("name");
        doReturn(recordIdValue("organization", organizationId)).when(doc).get("organization_id");
        return doc;
    }

    private Response responseWithOne(Object document) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(1);
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(document);
        when(array.get(0)).thenReturn(item);
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Response emptyResponse() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    private ApplicationData application() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(UUID.fromString(ORGANIZATION_ID));
        organization.setName("UCO");
        return ApplicationData.build(UUID.fromString(APPLICATION_ID), "App", organization);
    }

    private EnvironmentData environment(final String environmentId, final ApplicationData application,
                                        final String typeId) {
        return new EnvironmentData(UUID.fromString(environmentId), application,
                new EnvironmentTypeData(UUID.fromString(typeId), "Develop"));
    }

    private List<EnvironmentData> threeEnvironments(final ApplicationData application) {
        return List.of(
                environment("323e4567-e89b-12d3-a456-426614174000", application,
                        "423e4567-e89b-12d3-a456-426614174000"),
                environment("323e4567-e89b-12d3-a456-426614174001", application,
                        "423e4567-e89b-12d3-a456-426614174002"),
                environment("323e4567-e89b-12d3-a456-426614174003", application,
                        "423e4567-e89b-12d3-a456-426614174004"));
    }

    private String expectedTransaction(final List<EnvironmentData> environments) {
        final String applicationUpsert = "CREATE application:`" + APPLICATION_ID + "` CONTENT { "
                + "name: 'App', organization_id: organization:`" + ORGANIZATION_ID + "`, "
                + "language_id: language_base:`" + LANGUAGE_ID + "`, "
                + "state_id: application_state:`" + STATE_ID + "` };";
        final StringBuilder transaction = new StringBuilder("BEGIN TRANSACTION;")
                .append(applicationUpsert);
        environments.forEach(environment -> transaction
                .append("UPSERT environment:`").append(environment.getId()).append("` CONTENT { ")
                .append("application_id: application:`").append(APPLICATION_ID).append("`, ")
                .append("type_id: environment_type:`").append(environment.getType().getId()).append("`, ")
                .append("state_id: ").append(ENVIRONMENT_STATE_RECORD)
                .append(" };"));
        return transaction.append("COMMIT TRANSACTION;").toString();
    }

    private static long countOccurrences(final String text, final String token) {
        return text.split(Pattern.quote(token), -1).length - 1L;
    }

    @Test
    void findByName_returnsApplication_whenDocumentFound() {
        String uuid = UUID.randomUUID().toString();
        String organizationId = UUID.randomUUID().toString();
        doReturn(responseWithOne(applicationDocument(uuid, "App", organizationId))).when(surreal).query(anyString());

        Optional<ApplicationData> result = adapter.findByName("App");

        assertThat(result).hasValueSatisfying(application -> assertSoftly(softly -> {
            softly.assertThat(application.getName()).isEqualTo("App");
            softly.assertThat(application.getId()).isEqualTo(UUID.fromString(uuid));
            softly.assertThat(application.getOrganization().getId()).isEqualTo(UUID.fromString(organizationId));
        }));
    }

    @Test
    void findByName_returnsEmpty_whenResponseEmpty() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.findByName("App")).isEmpty();
    }

    @Test
    void findById_returnsMappedApplicationAndUsesExactRecordQuery() {
        String applicationId = "123e4567-e89b-12d3-a456-426614174000";
        String organizationId = "223e4567-e89b-12d3-a456-426614174000";
        String query = "SELECT *, organization_id.name AS organization_name FROM application:`" + applicationId + "` LIMIT 1;";
        doReturn(responseWithOne(applicationDocument(applicationId, "App", organizationId)))
                .when(surreal).query(query);

        Optional<ApplicationData> result = adapter.findById(applicationId);

        assertThat(result).hasValueSatisfying(application -> assertSoftly(softly -> {
            softly.assertThat(application.getId()).isEqualTo(UUID.fromString(applicationId));
            softly.assertThat(application.getName()).isEqualTo("App");
            softly.assertThat(application.getOrganization().getId()).isEqualTo(UUID.fromString(organizationId));
            softly.assertThat(application.getOrganization().getName()).isEmpty();
        }));
        verify(surreal).query(query);
    }

    @Test
    void findById_returnsEmpty_whenResponseEmpty() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.findById("123e4567-e89b-12d3-a456-426614174000")).isEmpty();
    }

    @Test
    void findById_propagatesFailureAndLogsQuery_whenQueryFails() {
        String applicationId = "123e4567-e89b-12d3-a456-426614174000";
        String query = "SELECT *, organization_id.name AS organization_name FROM application:`" + applicationId + "` LIMIT 1;";
        RuntimeException cause = new RuntimeException("db down");
        doThrow(cause).when(surreal).query(query);

        assertThatThrownBy(() -> adapter.findById(applicationId))
                .isSameAs(cause);
        verify(log).error(eq("Error al consultar aplicación por id en SurrealDB: " + query),
                any(RuntimeException.class));
    }

    @Test
    void existsById_returnsTrue_whenDocumentFound() {
        doReturn(responseWithOne(mock(Object.class))).when(surreal).query(anyString());

        assertThat(adapter.existsById("id-1")).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenResponseEmpty() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.existsById("id-1")).isFalse();
    }

    @Test
    @DisplayName("Persiste la aplicación y sus tres ambientes en una única transacción SurrealQL")
    void createWithEnvironments_executesOneTransactionWithApplicationAndThreeEnvironmentUpserts() {
        ApplicationData application = application();
        List<EnvironmentData> environments = threeEnvironments(application);

        adapter.createWithEnvironments(application, LANGUAGE_ID, STATE_ID, environments,
                ENVIRONMENT_STATE_ID);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(queryCaptor.capture());
        String query = queryCaptor.getValue();
        assertAll(
                () -> assertThat(query).startsWith("BEGIN TRANSACTION;"),
                () -> assertThat(query).endsWith("COMMIT TRANSACTION;"),
                () -> assertThat(countOccurrences(query, "BEGIN TRANSACTION;")).isEqualTo(1),
                () -> assertThat(countOccurrences(query, "COMMIT TRANSACTION;")).isEqualTo(1),
                () -> assertThat(countOccurrences(query, "CREATE application:`")).isEqualTo(1),
                () -> assertThat(countOccurrences(query, "UPSERT environment:`")).isEqualTo(3),
                () -> assertThat(countOccurrences(query, ENVIRONMENT_STATE_RECORD)).isEqualTo(3),
                () -> assertThat(query).contains("language_id: language_base:`" + LANGUAGE_ID + "`"),
                () -> assertThat(query).contains("state_id: application_state:`" + STATE_ID + "`"),
                () -> assertThat(query).contains("organization_id: organization:`" + ORGANIZATION_ID + "`"));
        verify(log).info("Creating application and {} default environments atomically", 3);
        verifyNoMoreInteractions(surreal);
    }

    @Test
    @DisplayName("Genera el statement exacto con los ids de ambiente, tipo y estado correctos")
    void createWithEnvironments_buildsExactSurrealqlStatement() {
        ApplicationData application = application();
        List<EnvironmentData> environments = threeEnvironments(application);
        String expectedQuery = expectedTransaction(environments);

        adapter.createWithEnvironments(application, LANGUAGE_ID, STATE_ID, environments,
                ENVIRONMENT_STATE_ID);

        verify(surreal).query(expectedQuery);
        verifyNoMoreInteractions(surreal);
    }

    @Test
    @DisplayName("Lanza BusinessException técnica cuando la transacción SurrealDB falla")
    void createWithEnvironments_throwsTechnicalBusinessException_whenQueryFails() {
        ApplicationData application = application();
        RuntimeException cause = new RuntimeException("db down");
        doThrow(cause).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.createWithEnvironments(application, LANGUAGE_ID, STATE_ID,
                threeEnvironments(application), ENVIRONMENT_STATE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertSoftly(softly -> {
                    softly.assertThat(((BusinessException) ex).getTechnicalMessage())
                            .isEqualTo("Error al persistir la aplicación y sus ambientes en la base de datos SurrealDB");
                    softly.assertThat(((BusinessException) ex).getRootException()).isSameAs(cause);
                }));
        verify(log).error(eq("Error al persistir la aplicación y sus ambientes en SurrealDB"),
                any(RuntimeException.class));
    }
}
