package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvironmentReferenceCatalogSurrealAdapterTest {

    private static final String TYPES_QUERY = "SELECT * FROM environment_type;";
    private static final String TYPES_ERROR =
            "Error al consultar tipos de ambiente en SurrealDB: " + TYPES_QUERY;

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private EnvironmentReferenceCatalogSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(EnvironmentReferenceCatalogSurrealAdapter.class)).thenReturn(log);
        adapter = new EnvironmentReferenceCatalogSurrealAdapter(surreal, loggerFactory);
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

    private Object documentWith(final String table, final String uuid, final String name) {
        Object doc = mock(Object.class);
        doReturn(recordIdValue(table, uuid)).when(doc).get("id");
        doReturn(stringValue(name)).when(doc).get("name");
        return doc;
    }

    private Object stateDocument(final String stateId) {
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment_state", stateId)).when(doc).get("id");
        return doc;
    }

    private Response responseWith(final List<Object> documents) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(documents.size());
        for (int index = 0; index < documents.size(); index++) {
            Value item = mock(Value.class);
            when(item.isObject()).thenReturn(true);
            when(item.getObject()).thenReturn(documents.get(index));
            when(array.get(index)).thenReturn(item);
        }
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Response responseWithOne(final Object document) {
        return responseWith(List.of(document));
    }

    private Response emptyResponse() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    @Test
    @DisplayName("Consulta la tabla environment_type y mapea id y nombre")
    void findAllTypes_mapsEnvironmentTypeRecords() {
        String developId = UUID.randomUUID().toString();
        String testingId = UUID.randomUUID().toString();
        doReturn(responseWith(List.of(
                        documentWith("environment_type", developId, "Develop"),
                        documentWith("environment_type", testingId, "Testing"))))
                .when(surreal).query(TYPES_QUERY);

        List<EnvironmentTypeData> types = adapter.findAllTypes();

        assertAll(
                () -> assertThat(types).hasSize(2),
                () -> assertThat(types.get(0).getId()).isEqualTo(UUID.fromString(developId)),
                () -> assertThat(types.get(0).getName()).isEqualTo("Develop"),
                () -> assertThat(types.get(1).getId()).isEqualTo(UUID.fromString(testingId)),
                () -> assertThat(types.get(1).getName()).isEqualTo("Testing"));
        verify(surreal).query(TYPES_QUERY);
    }

    @Test
    @DisplayName("Devuelve lista vacía cuando la consulta de tipos no arroja filas")
    void findAllTypes_returnsEmptyList_whenResponseEmpty() {
        doReturn(emptyResponse()).when(surreal).query(TYPES_QUERY);

        assertThat(adapter.findAllTypes()).isEmpty();
    }

    @Test
    @DisplayName("Registra y propaga el fallo al consultar los tipos de ambiente")
    void findAllTypes_propagatesFailureAndLogsQuery_whenQueryFails() {
        RuntimeException cause = new RuntimeException("connection refused");
        doThrow(cause).when(surreal).query(TYPES_QUERY);

        assertThatThrownBy(() -> adapter.findAllTypes()).isSameAs(cause);
        verify(log).error(eq(TYPES_ERROR), any(RuntimeException.class));
    }

    @ParameterizedTest(name = "consulta {0} comparando el nombre en minúsculas")
    @ValueSource(strings = {"Active", "InAcTiVe"})
    void findStateIdByName_usesLowercaseComparisonOnBothSides(String stateName) {
        String stateId = UUID.randomUUID().toString();
        String expectedQuery = "SELECT * FROM environment_state"
                + " WHERE string::lowercase(name) = string::lowercase('" + stateName + "') LIMIT 1;";
        doReturn(responseWithOne(stateDocument(stateId)))
                .when(surreal).query(expectedQuery);

        Optional<UUID> result = adapter.findStateIdByName(stateName);

        assertThat(result).contains(UUID.fromString(stateId));
        verify(surreal).query(expectedQuery);
    }

    @Test
    @DisplayName("Devuelve Optional vacío cuando el estado de ambiente no existe")
    void findStateIdByName_returnsEmpty_whenStateIsNotFound() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.findStateIdByName("Active")).isEmpty();
    }

    @Test
    @DisplayName("Registra y propaga el fallo al consultar el estado de ambiente")
    void findStateIdByName_propagatesFailureAndLogsQuery_whenQueryFails() {
        RuntimeException cause = new RuntimeException("connection refused");
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        doThrow(cause).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.findStateIdByName("Active")).isSameAs(cause);

        verify(surreal).query(queryCaptor.capture());
        verify(log).error(eq("Error al consultar estado de ambiente en SurrealDB: " + queryCaptor.getValue()),
                any(RuntimeException.class));
        assertThat(queryCaptor.getValue()).contains("string::lowercase(name) = string::lowercase('Active')");
    }
}
