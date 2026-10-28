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
        return oneRow(code, title, content, null, null);
    }

    private Response oneRow(String code, String title, String content, Value createdAt, Value updatedAt) {
        Object row = mock(Object.class);
        Value codeValue = stringValue(code);
        Value titleValue = stringValue(title);
        Value contentValue = stringValue(content);
        Value typeValue = stringValue("type");
        Value categoryValue = stringValue("category");
        Value functionalityValue = stringValue("functionality");
        Value applicationValue = stringValue("123e4567-e89b-12d3-a456-426614175030");
        Value idValue = stringValue("message_data_collection:123e4567-e89b-12d3-a456-426614175030");
        when(row.get("code")).thenReturn(codeValue);
        when(row.get("title")).thenReturn(titleValue);
        when(row.get("content")).thenReturn(contentValue);
        when(row.get("id")).thenReturn(idValue);
        when(row.get("type")).thenReturn(typeValue);
        when(row.get("category")).thenReturn(categoryValue);
        when(row.get("functionality")).thenReturn(functionalityValue);
        when(row.get("application_id")).thenReturn(applicationValue);
        when(row.get("created_at")).thenReturn(createdAt);
        when(row.get("updated_at")).thenReturn(updatedAt);
        return response(row);
    }

    private Response availabilityRow(Value updatedAt) {
        Object row = mock(Object.class);
        Value idValue = stringValue("message_data_collection:123e4567-e89b-12d3-a456-426614175030");
        when(row.get("id")).thenReturn(idValue);
        when(row.get("updated_at")).thenReturn(updatedAt);
        return response(row);
    }

    private Response response(Object row) {
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
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(text);
        return value;
    }

    @Test
    void findAvailableByCodeAndEnvironment_mapsFullProjectionAndFiltersActiveEnvironmentAndApplication() {
        doReturn(oneRow("CODE", "Title", "Body")).when(surreal).query(anyString());

        var result = adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid");

        assertThat(result).hasValueSatisfying(message -> assertThat(message)
                .extracting("id", "application", "code", "title", "content", "type.name", "category.name",
                        "functionality.name", "createdAt", "updatedAt")
                .containsExactly(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614175030"),
                        "123e4567-e89b-12d3-a456-426614175030", "CODE", "Title", "Body", "type", "category",
                        "functionality", null, null));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("environment:`environment-uuid`", "message_id.code = 'CODE'",
                "message_id.application_id = environment_id.application_id",
                "string::lowercase(message_id.status_id.name) IN ['activo', 'active']",
                "string::lowercase(state_data_id.name) IN ['activo', 'active']")
                .startsWith("SELECT message_id AS id, message_id.code AS code, message_id.title AS title, message_id.content AS content")
                .contains("message_id.type_id.name AS type", "message_id.category_id.name AS category",
                        "message_id.functionality_id.name AS functionality", "message_id.created_at AS created_at",
                        "message_id.updated_at AS updated_at");
    }

    @Test
    void findAvailableByCodeAndEnvironment_mapsDateTimeAndIsoStringDates() {
        java.time.Instant created = java.time.Instant.parse("2025-01-02T03:04:05Z");
        java.time.Instant updated = java.time.Instant.parse("2025-02-03T04:05:06Z");
        Value createdValue = mock(Value.class);
        when(createdValue.isDateTime()).thenReturn(true);
        when(createdValue.getDateTime()).thenReturn(java.time.ZonedDateTime.ofInstant(created, java.time.ZoneOffset.UTC));
        Value updatedValue = mock(Value.class);
        when(updatedValue.getString()).thenReturn(updated.toString());
        doReturn(oneRow("CODE", "Title", "Body", createdValue, updatedValue)).when(surreal).query(anyString());

        var result = adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid");

        assertThat(result).hasValueSatisfying(message -> assertThat(message)
                .extracting("createdAt", "updatedAt").containsExactly(created, updated));
    }

    @Test
    void findAvailableByCodeAndEnvironment_treatsNullAndNoneDatesAsNull() {
        Value nullValue = mock(Value.class);
        when(nullValue.isNull()).thenReturn(true);
        Value noneValue = mock(Value.class);
        when(noneValue.isNone()).thenReturn(true);
        doReturn(oneRow("CODE", "Title", "Body", nullValue, noneValue)).when(surreal).query(anyString());

        var result = adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid");

        assertThat(result).hasValueSatisfying(message -> assertThat(message)
                .extracting("createdAt", "updatedAt").containsExactly(null, null));
    }

    @Test
    void findAvailabilityByCodeAndEnvironment_usesMinimalSelectAndMapsIdAndUpdatedAt() {
        java.time.Instant updated = java.time.Instant.parse("2025-03-04T05:06:07Z");
        Value date = mock(Value.class);
        when(date.getString()).thenReturn(updated.toString());
        doReturn(availabilityRow(date)).when(surreal).query(anyString());

        var availability = adapter.findAvailabilityByCodeAndEnvironment("CODE", "environment-uuid");

        assertThat(availability).hasValueSatisfying(value -> assertThat(value)
                .extracting("messageId", "updatedAt")
                .containsExactly(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614175030"), updated));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).startsWith("SELECT message_id AS id, message_id.updated_at AS updated_at")
                .contains("message_id.application_id = environment_id.application_id",
                        "string::lowercase(message_id.status_id.name) IN ['activo', 'active']",
                        "string::lowercase(state_data_id.name) IN ['activo', 'active']");
    }

    @Test
    void findAvailabilityByCodeAndEnvironment_preservesDomainException() {
        CrossWordsException expected = CrossWordsException.buildInfrastructure("domain failure", "db failure",
                new IllegalStateException("root"), co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.TECHNICAL);
        doThrow(expected).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.findAvailabilityByCodeAndEnvironment("CODE", "environment-uuid"))
                .isSameAs(expected);
    }

    @Test
    void findAvailableByCodeAndEnvironment_preservesDomainException() {
        CrossWordsException expected = CrossWordsException.buildInfrastructure("domain failure", "db failure",
                new IllegalStateException("root"), co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.TECHNICAL);
        doThrow(expected).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.findAvailableByCodeAndEnvironment("CODE", "environment-uuid"))
                .isSameAs(expected);
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
