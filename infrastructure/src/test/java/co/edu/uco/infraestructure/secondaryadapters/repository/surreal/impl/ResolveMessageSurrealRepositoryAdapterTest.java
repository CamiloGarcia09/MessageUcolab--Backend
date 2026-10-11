package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveMessageSurrealRepositoryAdapterTest {
    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort log;
    @Mock private CatalogPort catalog;

    private ResolveMessageSurrealRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(ResolveMessageSurrealRepositoryAdapter.class)).thenReturn(log);
        when(catalog.getMessage("TCH_058")).thenReturn("query failed: %s");
        adapter = new ResolveMessageSurrealRepositoryAdapter(surreal, loggerFactory, catalog);
    }

    private Response oneRow(String code, String title, String content) {
        Object row = mock(Object.class);
        Value codeValue = stringValue(code);
        Value titleValue = stringValue(title);
        Value contentValue = stringValue(content);
        when(row.get("code")).thenReturn(codeValue);
        when(row.get("title")).thenReturn(titleValue);
        when(row.get("content")).thenReturn(contentValue);
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(row);
        Array rows = mock(Array.class);
        when(rows.len()).thenReturn(1);
        when(rows.get(0)).thenReturn(item);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(rows);
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Value stringValue(String text) {
        Value value = mock(Value.class);
        when(value.isNull()).thenReturn(false);
        when(value.isNone()).thenReturn(false);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(text);
        return value;
    }

    @Test
    void findAvailableByCodeAndEnvironment_mapsMinimalProjectionAndFiltersActiveEnvironmentAndApplication() {
        doReturn(oneRow("CODE", "Title", "Body")).when(surreal).query(anyString());

        var result = adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid");

        assertThat(result).hasValueSatisfying(message -> assertThat(message)
                .extracting("code", "title", "content").containsExactly("CODE", "Title", "Body"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("environment:`environment-uuid`", "message_id.code = 'CODE'",
                "message_id.application_id = environment_id.application_id",
                "message_id.status_id.name = 'Active'", "state_data_id.name = 'Active'")
                .startsWith("SELECT message_id.code AS code, message_id.title AS title, message_id.content AS content")
                .doesNotContain("created_at", "application AS");
    }

    @Test
    void findAvailableByCodeAndEnvironment_escapesCodeAndReturnsEmpty_whenNoRows() {
        Response empty = mock(Response.class);
        when(empty.size()).thenReturn(0);
        doReturn(empty).when(surreal).query(anyString());

        var result = adapter.findAvailableByCodeAndEnvironment("CO'DE", "environment-uuid");

        assertThat(result).isEmpty();
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("message_id.code = 'CO\\'DE'");
    }

    @Test
    void findAvailableByCodeAndEnvironment_wrapsDriverFailureAsInfrastructureException() {
        doThrow(new IllegalStateException("database unavailable")).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(exception -> assertThat(((CrossWordsException) exception).getHttpStatus()).isEqualTo(500));
    }
}
