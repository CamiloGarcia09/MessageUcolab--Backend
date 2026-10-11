package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import com.surrealdb.Array;
import com.surrealdb.Entry;
import com.surrealdb.Id;
import com.surrealdb.Object;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class SurrealDomainEventProjectionConsumerTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private SurrealDomainEventProjectionConsumer consumer;

    @BeforeEach
    void setUp() {
        lenient().when(loggerFactory.getLogger(SurrealDomainEventProjectionConsumer.class)).thenReturn(log);
        lenient().when(catalogPort.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("msg");
        CatalogPortStaticRef.set(catalogPort);
        consumer = new SurrealDomainEventProjectionConsumer(surreal, 2, loggerFactory);
        lenient().doReturn(mock(Response.class)).when(surreal).query(anyString());
    }

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    private Value stringValue(String value) {
        Value v = mock(Value.class);
        when(v.isNull()).thenReturn(false);
        when(v.isNone()).thenReturn(false);
        when(v.isString()).thenReturn(true);
        when(v.getString()).thenReturn(value);
        return v;
    }

    private Value longValue(long value) {
        Value v = mock(Value.class);
        when(v.isNull()).thenReturn(false);
        when(v.isNone()).thenReturn(false);
        when(v.isLong()).thenReturn(true);
        when(v.getLong()).thenReturn(value);
        return v;
    }

    private Response responseWith(Object... docs) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(docs.length);
        if (docs.length == 0) {
            return response;
        }
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(docs.length);
        for (int i = 0; i < docs.length; i++) {
            Value item = mock(Value.class);
            when(item.isObject()).thenReturn(true);
            when(item.getObject()).thenReturn((Object) docs[i]);
            when(array.get(i)).thenReturn(item);
        }
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Object eventObject(String id, String eventType, String aggregateId, String aggregateType, long version) {
        Object obj = mock(Object.class);
        doReturn(stringValue("domain_events:" + id)).when(obj).get("id");
        doReturn(stringValue(eventType)).when(obj).get("event_type");
        doReturn(stringValue(aggregateId)).when(obj).get("aggregate_id");
        doReturn(stringValue(aggregateType)).when(obj).get("aggregate_type");
        doReturn(longValue(version)).when(obj).get("version");
        when(obj.get("payload")).thenReturn(null);
        when(obj.get("metadata")).thenReturn(null);
        when(obj.get("created_at")).thenReturn(null);
        return obj;
    }

    private void stubEvents(Object... events) {
        doReturn(responseWith(events)).when(surreal).query(contains("FROM domain_events WHERE"));
    }

    private void stubRecord(String recordId, Object recordObj) {
        int separator = recordId.indexOf(':');
        String table = recordId.substring(0, separator);
        String id = recordId.substring(separator + 1);
        doReturn(responseWith(recordObj)).when(surreal).query(contains("FROM " + table + ":`" + id + "`"));
    }

    private Object catalogRecord(String name) {
        Object obj = mock(Object.class);
        doReturn(stringValue(name)).when(obj).get("name");
        return obj;
    }

    private Object applicationRecord() {
        Object obj = mock(Object.class);
        doReturn(stringValue("APP")).when(obj).get("name");
        return obj;
    }

    private Object environmentRecord() {
        Object environment = mock(Object.class);
        doReturn(stringValue("environment_type:et-1")).when(environment).get("type_id");
        return environment;
    }

    private String documentQuery(final String prefix) {
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, atLeastOnce()).query(queryCaptor.capture());
        return queryCaptor.getAllValues().stream()
                .filter(query -> query.startsWith(prefix))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void consumePendingDomainEvents_doesNothing_whenNoEvents() {
        stubEvents();

        consumer.consumePendingDomainEvents();

        verify(surreal).query(contains("FROM domain_events WHERE"));
    }

    @Test
    void consumePendingDomainEvents_deletesDocumentsForDeleteEvent() {
        stubEvents(eventObject("event-1", "APPLICATION_DELETED", "application:app-1", "application", 3));

        consumer.consumePendingDomainEvents();

        verify(surreal).query(contains("DELETE application_document:`app-1`"));
        verify(surreal).query(contains("SET projection_status = 'processed'"));
        verify(log).debug("msg");
    }

    @Test
    void consumePendingDomainEvents_projectsApplicationDocument() {
        Object app = mock(Object.class);
        doReturn(stringValue("organization:org-1")).when(app).get("organization_id");
        doReturn(stringValue("language:lang-1")).when(app).get("language_id");
        doReturn(stringValue("state:st-1")).when(app).get("state_id");
        doReturn(stringValue("MiApp")).when(app).get("name");
        when(app.get("created_at")).thenReturn(null);
        when(app.get("updated_at")).thenReturn(null);

        Object language = mock(Object.class);
        doReturn(stringValue("ES")).when(language).get("language");
        doReturn(stringValue("ES")).when(language).get("code");

        stubEvents(eventObject("event-1", "APPLICATION_CREATED", "application:app-1", "application", 1));
        stubRecord("application:app-1", app);
        stubRecord("organization:org-1", catalogRecord("UCO"));
        stubRecord("language:lang-1", language);
        stubRecord("state:st-1", catalogRecord("ACTIVE"));

        consumer.consumePendingDomainEvents();

        verify(surreal).query(contains("UPSERT application_document:`app-1`"));
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, atLeastOnce()).query(queryCaptor.capture());
        String applicationUpsert = queryCaptor.getAllValues().stream()
                .filter(query -> query.startsWith("UPSERT application_document:`app-1`"))
                .findFirst()
                .orElseThrow();
        assertThat(applicationUpsert)
                .contains("organization_id: 'organization:org-1'")
                .contains("organization: { id: 'organization:org-1', name: 'UCO' }")
                .contains("name: 'MiApp'")
                .doesNotContain(", version: ")
                .doesNotContain("start_date");
        verify(surreal).query(contains("UPSERT domain_event_document:`event-1`"));
        verify(surreal).query(contains("SET projection_status = 'processed'"));
    }

    @Test
    void consumePendingDomainEvents_deletesAggregateDocument_whenAggregateMissing() {
        stubEvents(eventObject("event-1", "APPLICATION_CREATED", "application:missing-1", "application", 1));

        consumer.consumePendingDomainEvents();

        verify(surreal).query(contains("DELETE application_document:`missing-1`"));
    }

    @Test
    void consumePendingDomainEvents_projectsEnvironmentDocument() {
        Object env = mock(Object.class);
        doReturn(stringValue("application:app-1")).when(env).get("application_id");
        doReturn(stringValue("environment_type:et-1")).when(env).get("type_id");
        doReturn(stringValue("state:st-1")).when(env).get("state_id");
        when(env.get("created_at")).thenReturn(null);
        when(env.get("updated_at")).thenReturn(null);

        stubEvents(eventObject("event-1", "ENVIRONMENT_CREATED", "environment:env-1", "environment", 1));
        stubRecord("environment:env-1", env);
        stubRecord("application:app-1", applicationRecord());
        stubRecord("environment_type:et-1", catalogRecord("PRODUCTION"));
        stubRecord("state:st-1", catalogRecord("ACTIVE"));

        consumer.consumePendingDomainEvents();

        assertThat(documentQuery("UPSERT environment_document:`env-1`"))
                .contains("name: 'PRODUCTION'")
                .contains("type_id: 'environment_type:et-1'")
                .contains("projection_version: 1")
                .doesNotContain(", version: ");
    }

    @Test
    void consumePendingDomainEvents_projectsMessageDocument() {
        Object message = mock(Object.class);
        doReturn(stringValue("type:t-1")).when(message).get("type_id");
        doReturn(stringValue("category:c-1")).when(message).get("category_id");
        doReturn(stringValue("status:s-1")).when(message).get("status_id");
        doReturn(stringValue("application:a-1")).when(message).get("application_id");
        doReturn(stringValue("functionality:f-1")).when(message).get("functionality_id");
        doReturn(stringValue("CODE")).when(message).get("code");
        doReturn(stringValue("Title")).when(message).get("title");
        doReturn(stringValue("Content")).when(message).get("content");
        doReturn(stringValue("2020-03-04T05:06:07Z")).when(message).get("created_at");
        when(message.get("updated_at")).thenReturn(null);

        stubEvents(eventObject("event-1", "MESSAGE_CREATED", "message:msg-1", "message", 1));
        stubRecord("message:msg-1", message);
        stubRecord("type:t-1", catalogRecord("MESSAGE_TYPE"));
        stubRecord("category:c-1", catalogRecord("CATEGORY"));
        stubRecord("status:s-1", catalogRecord("ACTIVE"));
        stubRecord("application:a-1", applicationRecord());
        stubRecord("functionality:f-1", catalogRecord("FUNC"));

        consumer.consumePendingDomainEvents();

        assertThat(documentQuery("UPSERT message_data_collection:`msg-1`"))
                .contains("application: 'APP'")
                .contains("created_at: '2020-03-04T05:06:07Z'")
                .doesNotContain(", version: ");
    }

    @Test
    void consumePendingDomainEvents_projectsMessageEnvironmentReadModel() {
        Object message = mock(Object.class);
        doReturn(stringValue("type:t-1")).when(message).get("type_id");
        doReturn(stringValue("category:c-1")).when(message).get("category_id");
        doReturn(stringValue("status:s-1")).when(message).get("status_id");
        doReturn(stringValue("application:a-1")).when(message).get("application_id");
        doReturn(stringValue("functionality:f-1")).when(message).get("functionality_id");
        doReturn(stringValue("CODE")).when(message).get("code");
        doReturn(stringValue("Title")).when(message).get("title");
        doReturn(stringValue("Content")).when(message).get("content");

        Object readModel = mock(Object.class);
        doReturn(stringValue("message:msg-1")).when(readModel).get("message_id");
        doReturn(stringValue("environment:e-1")).when(readModel).get("environment_id");
        doReturn(stringValue("state:st-1")).when(readModel).get("state_data_id");
        when(readModel.get("created_at")).thenReturn(null);
        when(readModel.get("updated_at")).thenReturn(null);

        stubEvents(eventObject("event-1", "MESSAGE_ENVIRONMENT_CREATED",
                "message_environment:me-1", "message_environment", 1));
        stubRecord("message_environment:me-1", readModel);
        stubRecord("message:msg-1", message);
        stubRecord("environment:e-1", environmentRecord());
        stubRecord("environment_type:et-1", catalogRecord("PRODUCTION"));
        stubRecord("state:st-1", catalogRecord("ACTIVE"));
        stubRecord("type:t-1", catalogRecord("MESSAGE_TYPE"));
        stubRecord("category:c-1", catalogRecord("CATEGORY"));
        stubRecord("status:s-1", catalogRecord("ACTIVE"));
        stubRecord("application:a-1", applicationRecord());
        stubRecord("functionality:f-1", catalogRecord("FUNC"));

        consumer.consumePendingDomainEvents();

        assertThat(documentQuery("UPSERT message_environment_readmodel:`me-1`"))
                .contains("environment: { id: 'environment:e-1', name: 'PRODUCTION' }")
                .doesNotContain(", version: ");
    }

    @Test
    void consumePendingDomainEvents_resolvesApplicationNameFromCatalog_ignoringDenormalizedValue() {
        Object message = mock(Object.class);
        doReturn(stringValue("type:t-1")).when(message).get("type_id");
        doReturn(stringValue("category:c-1")).when(message).get("category_id");
        doReturn(stringValue("status:s-1")).when(message).get("status_id");
        doReturn(stringValue("application:a-1")).when(message).get("application_id");
        doReturn(stringValue("functionality:f-1")).when(message).get("functionality_id");
        doReturn(stringValue("CODE")).when(message).get("code");
        doReturn(stringValue("Title")).when(message).get("title");
        doReturn(stringValue("Content")).when(message).get("content");

        stubEvents(eventObject("event-1", "MESSAGE_CREATED", "message:msg-1", "message", 1));
        stubRecord("message:msg-1", message);
        stubRecord("type:t-1", catalogRecord("MESSAGE_TYPE"));
        stubRecord("category:c-1", catalogRecord("CATEGORY"));
        stubRecord("status:s-1", catalogRecord("ACTIVE"));
        stubRecord("application:a-1", applicationRecord());
        stubRecord("functionality:f-1", catalogRecord("FUNC"));

        consumer.consumePendingDomainEvents();

        assertThat(documentQuery("UPSERT message_data_collection:`msg-1`"))
                .contains("application: 'APP'")
                .doesNotContain("'DENORM_APP'");
    }

    @Test
    void consumePendingDomainEvents_projectsReferences_whenMessageAndEnvironmentAreMissing() {
        Object readModel = mock(Object.class);
        doReturn(stringValue("message:missing-1")).when(readModel).get("message_id");
        doReturn(stringValue("environment:e-1")).when(readModel).get("environment_id");
        doReturn(stringValue("state:st-1")).when(readModel).get("state_data_id");
        when(readModel.get("created_at")).thenReturn(null);
        when(readModel.get("updated_at")).thenReturn(null);

        stubEvents(eventObject("event-1", "MESSAGE_ENVIRONMENT_CREATED",
                "message_environment:me-1", "message_environment", 1));
        stubRecord("message_environment:me-1", readModel);
        stubRecord("state:st-1", catalogRecord("ACTIVE"));

        consumer.consumePendingDomainEvents();

        assertThat(documentQuery("UPSERT message_environment_readmodel:`me-1`"))
                .contains("environment: { id: 'environment:e-1', name: NONE }")
                .contains("message: { id: 'message:missing-1' }");
    }

    @Test
    void consumePendingDomainEvents_storesUnknownAggregateAsRawDocument() {
        stubEvents(eventObject("event-1", "ORDER_CREATED", "order:o-1", "order", 1));

        consumer.consumePendingDomainEvents();

        verify(surreal).query(contains("UPSERT domain_event_document:`event-1`"));
        verify(surreal).query(contains("SET projection_status = 'processed'"));
        verify(log, times(2)).debug("msg");
    }

    @Test
    void consumePendingDomainEvents_marksFailed_whenProjectionThrows() {
        stubEvents(eventObject("event-1", "APPLICATION_CREATED", "application:app-1", "application", 1));
        doThrow(new RuntimeException("boom"))
                .when(surreal).query(contains("UPSERT domain_event_document:`event-1`"));

        consumer.consumePendingDomainEvents();

        verify(log).warn(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(RuntimeException.class));
        verify(surreal).query(contains("projection_status = 'failed'"));
        verify(surreal).query(contains("projection_error = 'boom'"));
    }

    private static java.lang.Object invokeStatic(String methodName, Class<?>[] paramTypes, java.lang.Object... args) throws Exception {
        Method method = SurrealDomainEventProjectionConsumer.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private Value plainValue() {
        Value v = mock(Value.class);
        when(v.isNull()).thenReturn(false);
        when(v.isNone()).thenReturn(false);
        return v;
    }

    @Test
    void literal_handlesNullAndEmptyKinds() throws Exception {
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{null})).isEqualTo("NONE");

        Value nullValue = mock(Value.class);
        when(nullValue.isNull()).thenReturn(true);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{nullValue})).isEqualTo("NONE");

        Value noneValue = mock(Value.class);
        when(noneValue.isNone()).thenReturn(true);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{noneValue})).isEqualTo("NONE");
    }

    @Test
    void literal_rendersPrimitiveKinds() throws Exception {
        Value string = plainValue();
        when(string.isString()).thenReturn(true);
        when(string.getString()).thenReturn("abc");
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{string})).isEqualTo("'abc'");

        Value bool = plainValue();
        when(bool.isBoolean()).thenReturn(true);
        when(bool.getBoolean()).thenReturn(true);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{bool})).isEqualTo("true");

        Value longValue = plainValue();
        when(longValue.isLong()).thenReturn(true);
        when(longValue.getLong()).thenReturn(42L);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{longValue})).isEqualTo("42");

        Value doubleValue = plainValue();
        when(doubleValue.isDouble()).thenReturn(true);
        when(doubleValue.getDouble()).thenReturn(3.5);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{doubleValue})).isEqualTo("3.5");

        Value decimal = plainValue();
        when(decimal.isBigDecimal()).thenReturn(true);
        when(decimal.getBigDecimal()).thenReturn(new BigDecimal("2.50"));
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{decimal})).isEqualTo("2.50");

        Value uuid = plainValue();
        when(uuid.isUuid()).thenReturn(true);
        when(uuid.getUuid()).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{uuid}))
                .isEqualTo("'123e4567-e89b-12d3-a456-426614174000'");
    }

    @Test
    void literal_rendersRecordIdDateTimeArrayObjectAndFallback() throws Exception {
        Id id = mock(Id.class);
        when(id.toString()).thenReturn("id-1");
        RecordId recordId = mock(RecordId.class);
        when(recordId.getTable()).thenReturn("tbl");
        when(recordId.getId()).thenReturn(id);
        Value recordIdValue = plainValue();
        when(recordIdValue.isRecordId()).thenReturn(true);
        when(recordIdValue.getRecordId()).thenReturn(recordId);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{recordIdValue})).isEqualTo("'tbl:id-1'");

        Value dateTime = plainValue();
        when(dateTime.isDateTime()).thenReturn(true);
        when(dateTime.getDateTime()).thenReturn(
                ZonedDateTime.of(2025, 1, 1, 10, 30, 0, 0, ZoneOffset.ofHours(-5)));
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{dateTime}))
                .isEqualTo("d'2025-01-01T15:30:00Z'");

        Value arrayValue = plainValue();
        when(arrayValue.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(2);
        Value first = plainValue();
        when(first.isString()).thenReturn(true);
        when(first.getString()).thenReturn("a");
        Value second = plainValue();
        when(second.isLong()).thenReturn(true);
        when(second.getLong()).thenReturn(1L);
        when(array.get(0)).thenReturn(first);
        when(array.get(1)).thenReturn(second);
        when(arrayValue.getArray()).thenReturn(array);
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{arrayValue})).isEqualTo("['a', 1]");

        Value fallback = plainValue();
        when(fallback.toString()).thenReturn("custom");
        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{fallback})).isEqualTo("'custom'");
    }

    @Test
    void literal_rendersObjectLiteral() throws Exception {
        Entry first = mock(Entry.class);
        when(first.getKey()).thenReturn("a");
        Value firstValue = plainValue();
        when(firstValue.isString()).thenReturn(true);
        when(firstValue.getString()).thenReturn("1");
        when(first.getValue()).thenReturn(firstValue);

        Entry second = mock(Entry.class);
        when(second.getKey()).thenReturn("b c");
        Value secondValue = plainValue();
        when(secondValue.isString()).thenReturn(true);
        when(secondValue.getString()).thenReturn("2");
        when(second.getValue()).thenReturn(secondValue);

        Object obj = mock(Object.class);
        when(obj.iterator()).thenReturn(List.of(first, second).iterator());

        assertThat(invokeStatic("literal", new Class[]{Value.class}, new java.lang.Object[]{valueOf(obj)}))
                .isEqualTo("{ a: '1', `b c`: '2' }");
    }

    private Value valueOf(Object obj) {
        Value v = plainValue();
        when(v.isObject()).thenReturn(true);
        when(v.getObject()).thenReturn(obj);
        return v;
    }

    @Test
    void keyLiteral_escapesKeysThatAreNotIdentifiers() throws Exception {
        assertThat(invokeStatic("keyLiteral", new Class[]{String.class}, new java.lang.Object[]{"simple"})).isEqualTo("simple");
        assertThat(invokeStatic("keyLiteral", new Class[]{String.class}, new java.lang.Object[]{"with space"}))
                .isEqualTo("`with space`");
        assertThat(invokeStatic("keyLiteral", new Class[]{String.class}, new java.lang.Object[]{"a`b"}))
                .isEqualTo("`ab`");
        assertThat(invokeStatic("keyLiteral", new Class[]{String.class}, new java.lang.Object[]{null})).isEqualTo("``");
    }

    @Test
    void recordString_returnsStringOrRecursesIntoObject() throws Exception {
        Value string = plainValue();
        when(string.isString()).thenReturn(true);
        when(string.getString()).thenReturn("message:msg-1");
        assertThat(invokeStatic("recordString", new Class[]{Value.class}, new java.lang.Object[]{string})).isEqualTo("message:msg-1");

        assertThat((String) invokeStatic("recordString", new Class[]{Value.class}, new java.lang.Object[]{null})).isEmpty();

        Object obj = mock(Object.class);
        doReturn(stringValue("type:t-1")).when(obj).get("id");
        Value objectValue = valueOf(obj);
        when(objectValue.toString()).thenReturn("");
        assertThat(invokeStatic("recordString", new Class[]{Value.class}, new java.lang.Object[]{objectValue})).isEqualTo("type:t-1");
    }

    @Test
    void stringOf_handlesAllKinds() throws Exception {
        assertThat((String) invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{null})).isEmpty();

        Value none = mock(Value.class);
        when(none.isNone()).thenReturn(true);
        assertThat((String) invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{none})).isEmpty();

        Value string = plainValue();
        when(string.isString()).thenReturn(true);
        when(string.getString()).thenReturn("text");
        assertThat(invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{string})).isEqualTo("text");

        Id id = mock(Id.class);
        when(id.toString()).thenReturn("id-1");
        RecordId recordId = mock(RecordId.class);
        when(recordId.getTable()).thenReturn("tbl");
        when(recordId.getId()).thenReturn(id);
        Value rid = plainValue();
        when(rid.isRecordId()).thenReturn(true);
        when(rid.getRecordId()).thenReturn(recordId);
        assertThat(invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{rid})).isEqualTo("tbl:id-1");

        Value uuid = plainValue();
        when(uuid.isUuid()).thenReturn(true);
        when(uuid.getUuid()).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{uuid}))
                .isEqualTo("123e4567-e89b-12d3-a456-426614174000");

        Value dateTime = plainValue();
        when(dateTime.isDateTime()).thenReturn(true);
        when(dateTime.getDateTime()).thenReturn(
                ZonedDateTime.of(2025, 1, 1, 10, 30, 0, 0, ZoneOffset.ofHours(-5)));
        assertThat(invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{dateTime}))
                .isEqualTo("2025-01-01T15:30:00Z");

        Value fallback = plainValue();
        when(fallback.toString()).thenReturn("raw");
        assertThat(invokeStatic("stringOf", new Class[]{Value.class}, new java.lang.Object[]{fallback})).isEqualTo("raw");
    }

    @Test
    void longOf_handlesNumericKinds() throws Exception {
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{null})).isEqualTo(0L);

        Value longValue = plainValue();
        when(longValue.isLong()).thenReturn(true);
        when(longValue.getLong()).thenReturn(7L);
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{longValue})).isEqualTo(7L);

        Value doubleValue = plainValue();
        when(doubleValue.isDouble()).thenReturn(true);
        when(doubleValue.getDouble()).thenReturn(3.9);
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{doubleValue})).isEqualTo(3L);

        Value decimal = plainValue();
        when(decimal.isBigDecimal()).thenReturn(true);
        when(decimal.getBigDecimal()).thenReturn(new BigDecimal("10.9"));
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{decimal})).isEqualTo(10L);

        Value string = plainValue();
        when(string.isString()).thenReturn(true);
        when(string.getString()).thenReturn("5");
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{string})).isEqualTo(5L);

        Value invalid = plainValue();
        when(invalid.isString()).thenReturn(true);
        when(invalid.getString()).thenReturn("abc");
        assertThat(invokeStatic("longOf", new Class[]{Value.class}, new java.lang.Object[]{invalid})).isEqualTo(0L);
    }

    @Test
    void cleanIdPart_removesSurroundingDelimiters() throws Exception {
        assertThat((String) invokeStatic("cleanIdPart", new Class[]{String.class}, new java.lang.Object[]{null})).isEmpty();
        assertThat(invokeStatic("cleanIdPart", new Class[]{String.class}, new java.lang.Object[]{"`id-1`"})).isEqualTo("id-1");
        assertThat(invokeStatic("cleanIdPart", new Class[]{String.class}, new java.lang.Object[]{"\u27E8id-2\u27E9"})).isEqualTo("id-2");
        assertThat(invokeStatic("cleanIdPart", new Class[]{String.class}, new java.lang.Object[]{"plain"})).isEqualTo("plain");
    }

    @Test
    void recordIdPart_handlesRecordIdStringAndEmpty() throws Exception {
        assertThat((String) invokeStatic("recordIdPart", new Class[]{Value.class}, new java.lang.Object[]{null})).isEmpty();

        Id id = mock(Id.class);
        when(id.toString()).thenReturn("\u27E8ev-1\u27E9");
        RecordId recordId = mock(RecordId.class);
        when(recordId.getId()).thenReturn(id);
        Value rid = plainValue();
        when(rid.isRecordId()).thenReturn(true);
        when(rid.getRecordId()).thenReturn(recordId);
        assertThat(invokeStatic("recordIdPart", new Class[]{Value.class}, new java.lang.Object[]{rid})).isEqualTo("ev-1");

        Value string = plainValue();
        when(string.isString()).thenReturn(true);
        when(string.getString()).thenReturn("domain_events:ev-2");
        assertThat(invokeStatic("recordIdPart", new Class[]{Value.class}, new java.lang.Object[]{string})).isEqualTo("ev-2");

        Value stringNoRef = plainValue();
        when(stringNoRef.isString()).thenReturn(true);
        when(stringNoRef.getString()).thenReturn("plain-id");
        assertThat(invokeStatic("recordIdPart", new Class[]{Value.class}, new java.lang.Object[]{stringNoRef})).isEqualTo("plain-id");
    }

    @Test
    void recordRefFrom_parsesValidIdsAndRejectsInvalid() throws Exception {
        Class<?> recordRef = Class.forName(SurrealDomainEventProjectionConsumer.class.getName() + "$RecordRef");
        Method from = recordRef.getDeclaredMethod("from", String.class);
        from.setAccessible(true);
        Method table = recordRef.getDeclaredMethod("table");
        table.setAccessible(true);
        Method id = recordRef.getDeclaredMethod("id");
        id.setAccessible(true);

        java.lang.Object ref = from.invoke(null, "message:msg-1");
        java.util.Optional<?> optRef = (java.util.Optional<?>) ref;
        assertThat(optRef).isPresent();
        assertThat(table.invoke(optRef.get())).isEqualTo("message");
        assertThat(id.invoke(optRef.get())).isEqualTo("msg-1");

        java.lang.Object cleaned = from.invoke(null, " message : \u27E8id-2\u27E9 ");
        java.util.Optional<?> optCleaned = (java.util.Optional<?>) cleaned;
        assertThat(optCleaned).isPresent();
        assertThat(id.invoke(optCleaned.get())).isEqualTo("id-2");

        assertThat(((java.util.Optional<?>) from.invoke(null, new java.lang.Object[]{null}))).isEmpty();
        assertThat(((java.util.Optional<?>) from.invoke(null, "  "))).isEmpty();
        assertThat(((java.util.Optional<?>) from.invoke(null, "no-separator"))).isEmpty();
        assertThat(((java.util.Optional<?>) from.invoke(null, ":id"))).isEmpty();
        assertThat(((java.util.Optional<?>) from.invoke(null, "table:"))).isEmpty();
    }

    @Test
    void domainEventFrom_mapsFieldsAndDetectsDeletes() throws Exception {
        Class<?> domainEvent = Class.forName(SurrealDomainEventProjectionConsumer.class.getName() + "$DomainEvent");
        Method from = domainEvent.getDeclaredMethod("from", Object.class);
        from.setAccessible(true);
        Method isDelete = domainEvent.getDeclaredMethod("isDelete");
        isDelete.setAccessible(true);
        Method fullId = domainEvent.getDeclaredMethod("fullId");
        fullId.setAccessible(true);

        java.lang.Object event = from.invoke(null, eventObject("ev-1", "APPLICATION_DELETED", "application:app-1", "application", 2));
        assertThat((boolean) isDelete.invoke(event)).isTrue();
        assertThat(fullId.invoke(event)).isEqualTo("domain_events:ev-1");

        java.lang.Object created = from.invoke(null, eventObject("ev-2", "APPLICATION_CREATED", "application:app-2", "application", 1));
        assertThat((boolean) isDelete.invoke(created)).isFalse();
    }

    @Test
    void constructor_clampsBatchSizeToAtLeastOne() {
        SurrealDomainEventProjectionConsumer clamped = new SurrealDomainEventProjectionConsumer(surreal, 0, loggerFactory);
        assertThat(clamped).isNotNull();
    }
}
