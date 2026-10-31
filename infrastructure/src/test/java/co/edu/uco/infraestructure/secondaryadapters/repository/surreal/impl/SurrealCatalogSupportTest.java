package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
class SurrealCatalogSupportTest {

    private static final UUID DEFAULT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPort log;

    private Support support;

    @BeforeEach
    void setUp() {
        support = new Support(surreal, log);
    }

    private static final class Support extends SurrealCatalogSupport {
        private Support(Surreal surreal, LoggingPort log) {
            super(surreal, log);
        }

        private <T> List<T> findAll(String table, String errorMessage, RowMapper<T> mapper) {
            return queryAll(table, errorMessage, mapper);
        }

        private <T> List<T> find(String sql, String errorMessage, RowMapper<T> mapper) {
            return query(sql, errorMessage, mapper);
        }

        private <T> Optional<T> findOne(String sql, String errorMessage, RowMapper<T> mapper) {
            return queryOne(sql, errorMessage, mapper);
        }
    }

    private Response emptyResponse() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    private Response statementResponse(Value statement) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Value notArrayStatement() {
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(false);
        return statement;
    }

    private Response arrayResponse(Value... items) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(items.length);
        for (int i = 0; i < items.length; i++) {
            when(array.get(i)).thenReturn(items[i]);
        }
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Value objectItem(Object document) {
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(true);
        doReturn(document).when(item).getObject();
        return item;
    }

    private Value nonObjectItem() {
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(false);
        return item;
    }

    @Test
    void query_returnsEmpty_whenResponseIsNull() {
        doReturn(null).when(surreal).query(anyString());

        assertThat(support.find("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void query_returnsEmpty_whenResponseSizeZero() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(support.find("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void query_returnsEmpty_whenStatementIsNull() {
        doReturn(statementResponse(null)).when(surreal).query(anyString());

        assertThat(support.find("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void query_returnsEmpty_whenStatementNotArray() {
        doReturn(statementResponse(notArrayStatement())).when(surreal).query(anyString());

        assertThat(support.find("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void query_returnsMappedList_whenArrayHasObjects() {
        Object doc = mock(Object.class);
        doReturn(arrayResponse(objectItem(doc), null, nonObjectItem())).when(surreal).query(anyString());

        List<Object> result = support.find("SELECT * FROM application;", "error", obj -> obj);

        assertThat(result).containsExactly(doc);
    }

    @Test
    void query_throwsRuntimeException_whenSurrealFails() {
        doThrow(new RuntimeException("db down")).when(surreal).query(anyString());

        assertThatThrownBy(() -> support.find("SELECT * FROM application;", "custom error", obj -> obj))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
        verify(log).error(eq("custom error"), any(RuntimeException.class));
    }

    @Test
    void queryAll_buildsSelectAllSql() {
        Object doc = mock(Object.class);
        doReturn(arrayResponse(objectItem(doc))).when(surreal).query(anyString());

        List<Object> result = support.findAll("application", "error", obj -> obj);

        assertThat(result).containsExactly(doc);
        verify(surreal).query("SELECT * FROM application;");
    }

    @Test
    void queryOne_returnsEmpty_whenResponseIsNull() {
        doReturn(null).when(surreal).query(anyString());

        assertThat(support.findOne("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void queryOne_returnsEmpty_whenResponseSizeZero() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(support.findOne("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void queryOne_returnsEmpty_whenStatementNotArray() {
        doReturn(statementResponse(notArrayStatement())).when(surreal).query(anyString());

        assertThat(support.findOne("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void queryOne_returnsEmpty_whenArrayIsEmpty() {
        doReturn(arrayResponse(new Value[0])).when(surreal).query(anyString());

        assertThat(support.findOne("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void queryOne_returnsEmpty_whenFirstItemIsNotObject() {
        doReturn(arrayResponse(nonObjectItem())).when(surreal).query(anyString());

        assertThat(support.findOne("SELECT * FROM application;", "error", obj -> obj)).isEmpty();
    }

    @Test
    void queryOne_returnsMappedValue_whenObjectFound() {
        Object doc = mock(Object.class);
        doReturn(arrayResponse(objectItem(doc)))
                .when(surreal).query(anyString());

        Optional<Object> result = support.findOne("SELECT * FROM application;", "error", obj -> obj);

        assertThat(result).contains(doc);
    }

    @Test
    void queryOne_throwsRuntimeException_whenSurrealFails() {
        doThrow(new RuntimeException("db down")).when(surreal).query(anyString());

        assertThatThrownBy(() -> support.findOne("SELECT * FROM application;", "custom error", obj -> obj))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
        verify(log).error(eq("custom error"), any(RuntimeException.class));
    }

    @Test
    void extractIdAsUUID_returnsDefault_whenValueIsNull() {
        assertThat(SurrealCatalogSupport.extractIdAsUUID(null)).isEqualTo(DEFAULT_UUID);
    }

    @Test
    void extractIdAsUUID_parsesRecordId() {
        UUID uuid = UUID.randomUUID();
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn("functionality:" + uuid);
        Value value = mock(Value.class);
        when(value.isRecordId()).thenReturn(true);
        when(value.getRecordId()).thenReturn(recordId);

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(uuid);
    }

    @Test
    void extractIdAsUUID_returnsDefault_whenRecordIdHasNoSeparator() {
        Value value = mock(Value.class);
        when(value.isRecordId()).thenReturn(true);
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn("not-a-uuid");
        when(value.getRecordId()).thenReturn(recordId);

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(DEFAULT_UUID);
    }

    @Test
    void extractIdAsUUID_returnsDefault_whenRecordIdThrows() {
        Value value = mock(Value.class);
        when(value.isRecordId()).thenReturn(true);
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenThrow(new IllegalStateException("boom"));
        when(value.getRecordId()).thenReturn(recordId);

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(DEFAULT_UUID);
    }

    @Test
    void extractIdAsUUID_parsesStringWithSeparator() {
        UUID uuid = UUID.randomUUID();
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn("message:" + uuid);

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(uuid);
    }

    @Test
    void extractIdAsUUID_parsesStringWithoutSeparator() {
        UUID uuid = UUID.randomUUID();
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(uuid.toString());

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(uuid);
    }

    @Test
    void extractIdAsUUID_returnsDefault_whenStringIsNotParseable() {
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn("not-a-uuid");

        assertThat(SurrealCatalogSupport.extractIdAsUUID(value)).isEqualTo(DEFAULT_UUID);
    }

    @Test
    void cleanThingId_removesBackticks() {
        assertThat(SurrealCatalogSupport.cleanThingId("`abc`")).isEqualTo("abc");
    }

    @Test
    void cleanThingId_removesAngleBrackets() {
        assertThat(SurrealCatalogSupport.cleanThingId("\u27E8abc\u27E9")).isEqualTo("abc");
    }

    @Test
    void cleanThingId_removesPartBeforeSeparator() {
        assertThat(SurrealCatalogSupport.cleanThingId("table:abc")).isEqualTo("abc");
    }

    @Test
    void extractCatalogId_returnsDefault_whenValueIsNull() {
        assertThat(SurrealCatalogSupport.extractCatalogId(null)).isEqualTo(DEFAULT_UUID);
    }

    @Test
    void extractCatalogId_parsesRecordId() {
        UUID uuid = UUID.randomUUID();
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn("message:" + uuid);
        Value value = mock(Value.class);
        when(value.isRecordId()).thenReturn(true);
        when(value.getRecordId()).thenReturn(recordId);

        assertThat(SurrealCatalogSupport.extractCatalogId(value)).isEqualTo(uuid);
    }

    @Test
    void extractCatalogId_parsesString() {
        UUID uuid = UUID.randomUUID();
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(uuid.toString());

        assertThat(SurrealCatalogSupport.extractCatalogId(value)).isEqualTo(uuid);
    }

    @Test
    void extractCatalogId_fallsBackToToString() {
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("catalog:custom");

        assertThat(SurrealCatalogSupport.extractCatalogId(value))
                .isEqualTo(UUID.nameUUIDFromBytes("catalog:custom".getBytes()));
    }

    @Test
    void stringOf_returnsEmpty_whenValueIsNull() {
        assertThat(SurrealCatalogSupport.stringOf(null)).isEmpty();
    }

    @Test
    void stringOf_returnsEmpty_whenValueIsNullValue() {
        Value value = mock(Value.class);
        when(value.isNull()).thenReturn(true);

        assertThat(SurrealCatalogSupport.stringOf(value)).isEmpty();
    }

    @Test
    void stringOf_returnsEmpty_whenValueIsNone() {
        Value value = mock(Value.class);
        when(value.isNone()).thenReturn(true);

        assertThat(SurrealCatalogSupport.stringOf(value)).isEmpty();
    }

    @Test
    void stringOf_returnsString_whenValueIsString() {
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn("ACTIVE");

        assertThat(SurrealCatalogSupport.stringOf(value)).isEqualTo("ACTIVE");
    }

    @Test
    void stringOf_returnsUuidString_whenValueIsUuid() {
        UUID uuid = UUID.randomUUID();
        Value value = mock(Value.class);
        when(value.isUuid()).thenReturn(true);
        when(value.getUuid()).thenReturn(uuid);

        assertThat(SurrealCatalogSupport.stringOf(value)).isEqualTo(uuid.toString());
    }

    @Test
    void stringOf_returnsToString_whenOtherValue() {
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("other");

        assertThat(SurrealCatalogSupport.stringOf(value)).isEqualTo("other");
    }
}