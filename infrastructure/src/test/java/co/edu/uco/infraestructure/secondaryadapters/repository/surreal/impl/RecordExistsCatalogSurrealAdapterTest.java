package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordExistsCatalogSurrealAdapterTest {

    private static final String CATALOG_UUID = "123e4567-e89b-12d3-a456-426614175801";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private RecordExistsCatalogSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(RecordExistsCatalogSurrealAdapter.class)).thenReturn(log);
        adapter = new RecordExistsCatalogSurrealAdapter(surreal, loggerFactory);
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

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ReferenceCatalog.class, names = {
            "MESSAGE_TYPE", "MESSAGE_CATEGORY", "MESSAGE_STATE", "MESSAGE_ENVIRONMENT_STATE"})
    void exists_returnsTrue_whenRecordFound(ReferenceCatalog catalog) {
        String query = "SELECT * FROM " + catalog.getTable() + ":`" + CATALOG_UUID + "` LIMIT 1;";
        doReturn(responseWithOne(mock(Object.class))).when(surreal).query(query);

        assertThat(adapter.exists(catalog, CATALOG_UUID)).isTrue();

        verify(surreal).query(query);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ReferenceCatalog.class, names = {
            "MESSAGE_TYPE", "MESSAGE_CATEGORY", "MESSAGE_STATE", "MESSAGE_ENVIRONMENT_STATE"})
    void exists_returnsFalse_whenResponseEmpty(ReferenceCatalog catalog) {
        String query = "SELECT * FROM " + catalog.getTable() + ":`" + CATALOG_UUID + "` LIMIT 1;";
        doReturn(emptyResponse()).when(surreal).query(query);

        assertThat(adapter.exists(catalog, CATALOG_UUID)).isFalse();

        verify(surreal).query(query);
    }

    @Test
    void exists_propagatesFailureAndLogsQuery_whenQueryFails() {
        ReferenceCatalog catalog = ReferenceCatalog.MESSAGE_TYPE;
        String query = "SELECT * FROM " + catalog.getTable() + ":`" + CATALOG_UUID + "` LIMIT 1;";
        RuntimeException cause = new RuntimeException("db down");
        doThrow(cause).when(surreal).query(query);

        assertThatThrownBy(() -> adapter.exists(catalog, CATALOG_UUID)).isSameAs(cause);

        verify(log).error(eq("Error al validar el registro de catálogo en SurrealDB: " + query),
                any(RuntimeException.class));
    }
}
