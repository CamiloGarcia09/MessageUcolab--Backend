package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import com.surrealdb.Array;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageSurrealRepositoryAdapterImplTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private MessageSurrealRepositoryAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageSurrealRepositoryAdapterImpl.class)).thenReturn(log);
        when(catalogPort.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("msg");
        CatalogPortStaticRef.set(catalogPort);
        adapter = new MessageSurrealRepositoryAdapterImpl(surreal, loggerFactory);
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

    private Value nameValue(String name) {
        Object obj = mock(Object.class);
        doReturn(stringValue(name)).when(obj).get("name");
        doReturn(recordIdValue("catalog", UUID.randomUUID().toString())).when(obj).get("id");
        Value v = mock(Value.class);
        when(v.isNull()).thenReturn(false);
        when(v.isNone()).thenReturn(false);
        when(v.isObject()).thenReturn(true);
        when(v.getObject()).thenReturn(obj);
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

    private Object messageDocument(String uuid, String code) {
        return messageDocument(uuid, code, null);
    }

    private Object messageDocument(String uuid, String code, Value createdAt) {
        Object doc = mock(Object.class);
        lenient().when(doc.get("message")).thenReturn(null);
        doReturn(recordIdValue("message", uuid)).when(doc).get("id");
        doReturn(stringValue(code)).when(doc).get("code");
        doReturn(stringValue("Title")).when(doc).get("title");
        doReturn(stringValue("Content")).when(doc).get("content");
        doReturn(stringValue("App")).when(doc).get("application");
        doReturn(nameValue("MESSAGE_TYPE")).when(doc).get("type");
        doReturn(nameValue("CATEGORY")).when(doc).get("category");
        doReturn(nameValue("ACTIVE")).when(doc).get("status");
        doReturn(nameValue("FUNC")).when(doc).get("functionality");
        doReturn(createdAt).when(doc).get("created_at");
        lenient().doReturn(null).when(doc).get("updated_at");
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

    @Test
    void findById_returnsMessage_whenDocumentFound() {
        String uuid = UUID.randomUUID().toString();
        doReturn(responseWithOne(messageDocument(uuid, "CODE-1"))).when(surreal).query(anyString());

        Optional<MessageData> result = adapter.findById(uuid);

        assertThat(result).isPresent();
        MessageData data = result.get();
        assertThat(data.getCode()).isEqualTo("CODE-1");
        assertThat(data.getType().getName()).isEqualTo("MESSAGE_TYPE");
        assertThat(data.getCategory().getName()).isEqualTo("CATEGORY");
        assertThat(data.getStatus().getName()).isEqualTo("ACTIVE");
    }

    @Test
    void findById_returnsEmpty_whenResponseNull() {
        when(surreal.query(anyString())).thenReturn(null);

        assertThat(adapter.findById("id-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenResponseSizeZero() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findById("id-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenStatementNotArray() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(false);
        when(response.take(0)).thenReturn(statement);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findById("id-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenArrayEmpty() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(0);
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findById("id-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenFirstNotObject() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(1);
        Value item = mock(Value.class);
        when(item.isObject()).thenReturn(false);
        when(array.get(0)).thenReturn(item);
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findById("id-1")).isEmpty();
    }

    @Test
    void findById_throwsAndLogs_whenQueryFails() {
        when(surreal.query(anyString())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> adapter.findById("id-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void findByCode_returnsMessage() {
        String uuid = UUID.randomUUID().toString();
        doReturn(responseWithOne(messageDocument(uuid, "CODE-2"))).when(surreal).query(anyString());

        Optional<MessageData> result = adapter.findByCode("CODE-2");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("CODE-2");
        assertThat(result.get().getId()).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    void findMessageByCodeAndEnvironment_usesEmbeddedMessageObject() {
        String uuid = UUID.randomUUID().toString();
        Object readModel = mock(Object.class);
        Object embedded = messageDocument(uuid, "CODE-3");
        Value embeddedValue = mock(Value.class);
        when(embeddedValue.isObject()).thenReturn(true);
        when(embeddedValue.getObject()).thenReturn(embedded);
        when(readModel.get("message")).thenReturn(embeddedValue);
        doReturn(responseWithOne(readModel)).when(surreal).query(anyString());

        Optional<MessageData> result = adapter.findMessageByCodeAndEnvironment("CODE-3", "env-1");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("CODE-3");
    }

    @Test
    void findMessagesByEnvironment_buildsPageWithCount() {
        String uuid = UUID.randomUUID().toString();
        Response countResponse = mock(Response.class);
        when(countResponse.size()).thenReturn(1);
        Value countStatement = mock(Value.class);
        when(countStatement.isArray()).thenReturn(true);
        Array countArray = mock(Array.class);
        when(countArray.len()).thenReturn(1);
        Value countItem = mock(Value.class);
        when(countItem.isObject()).thenReturn(true);
        Object countObj = mock(Object.class);
        when(countItem.getObject()).thenReturn(countObj);
        Value countValue = mock(Value.class);
        when(countValue.isNull()).thenReturn(false);
        when(countValue.isString()).thenReturn(true);
        when(countValue.getString()).thenReturn("3");
        when(countObj.get("count")).thenReturn(countValue);
        when(countArray.get(0)).thenReturn(countItem);
        when(countStatement.getArray()).thenReturn(countArray);
        when(countResponse.take(0)).thenReturn(countStatement);

        when(surreal.query(contains("COUNT()"))).thenReturn(countResponse);
        doReturn(responseWithOne(messageDocument(uuid, "CODE-4"))).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());

        assertThat(page.getTotalItems()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getCode()).isEqualTo("CODE-4");
    }

    @Test
    void findById_parsesIsoCreatedAt_whenDocumentContainsStringTimestamp() {
        String uuid = UUID.randomUUID().toString();
        Value timestamp = mock(Value.class);
        when(timestamp.isNull()).thenReturn(false);
        when(timestamp.isNone()).thenReturn(false);
        when(timestamp.isDateTime()).thenReturn(false);
        when(timestamp.getString()).thenReturn("2025-01-02T03:04:05Z");
        doReturn(responseWithOne(messageDocument(uuid, "CODE-1", timestamp)))
                .when(surreal).query(anyString());

        MessageData message = adapter.findById(uuid).orElseThrow();

        assertThat(message.getCreatedAt()).isEqualTo(java.time.Instant.parse("2025-01-02T03:04:05Z"));
    }

    @Test
    void findById_leavesCreatedAtNull_whenDocumentHasNoTimestamp() {
        String uuid = UUID.randomUUID().toString();
        doReturn(responseWithOne(messageDocument(uuid, "CODE-1"))).when(surreal).query(anyString());

        MessageData message = adapter.findById(uuid).orElseThrow();

        assertThat(message.getCreatedAt()).isNull();
    }

    @Test
    void findMessagesByEnvironment_doesNotUseGlobalFilter_whenEnvironmentIsMissing() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment(null, PageRequest.of(0, 2), new MessageFilter());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        List<String> queries = queryCaptor.getAllValues();
        assertThat(queries)
                .isNotEmpty()
                .allSatisfy(query -> assertThat(query)
                        .contains("environment_id")
                        .doesNotContain("WHERE TRUE"));
    }


    @Test
    void findMessagesByEnvironment_usesToStringCount_whenNotString() {
        Response countResponse = mock(Response.class);
        when(countResponse.size()).thenReturn(1);
        Value countStatement = mock(Value.class);
        when(countStatement.isArray()).thenReturn(true);
        Array countArray = mock(Array.class);
        when(countArray.len()).thenReturn(1);
        Value countItem = mock(Value.class);
        when(countItem.isObject()).thenReturn(true);
        Object countObj = mock(Object.class);
        when(countItem.getObject()).thenReturn(countObj);
        Value countValue = mock(Value.class);
        when(countValue.isNull()).thenReturn(false);
        when(countValue.isString()).thenReturn(false);
        when(countValue.toString()).thenReturn("5");
        when(countObj.get("count")).thenReturn(countValue);
        when(countArray.get(0)).thenReturn(countItem);
        when(countStatement.getArray()).thenReturn(countArray);
        when(countResponse.take(0)).thenReturn(countStatement);

        when(surreal.query(contains("COUNT()"))).thenReturn(countResponse);
        doReturn(mock(Response.class)).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());

        assertThat(page.getTotalItems()).isEqualTo(5);
        assertThat(page.getData()).isEmpty();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountInvalid() {
        Response countResponse = mock(Response.class);
        when(countResponse.size()).thenReturn(1);
        Value countStatement = mock(Value.class);
        when(countStatement.isArray()).thenReturn(true);
        Array countArray = mock(Array.class);
        when(countArray.len()).thenReturn(1);
        Value countItem = mock(Value.class);
        when(countItem.isObject()).thenReturn(true);
        Object countObj = mock(Object.class);
        when(countItem.getObject()).thenReturn(countObj);
        Value countValue = mock(Value.class);
        when(countValue.isNull()).thenReturn(false);
        when(countValue.isString()).thenReturn(false);
        when(countValue.toString()).thenReturn("abc");
        when(countObj.get("count")).thenReturn(countValue);
        when(countArray.get(0)).thenReturn(countItem);
        when(countStatement.getArray()).thenReturn(countArray);
        when(countResponse.take(0)).thenReturn(countStatement);

        when(surreal.query(contains("COUNT()"))).thenReturn(countResponse);
        doReturn(mock(Response.class)).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());

        assertThat(page.getTotalItems()).isZero();
        assertThat(page.getData()).isEmpty();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroWhenCountResponseEmpty() {
        Response empty = mock(Response.class);
        when(empty.size()).thenReturn(0);
        when(surreal.query(contains("COUNT()"))).thenReturn(empty);
        doReturn(mock(Response.class)).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());

        assertThat(page.getTotalItems()).isZero();
        assertThat(page.getData()).isEmpty();
    }

    @Test
    void findMessagesByEnvironment_rethrowsRuntimeException() {
        doReturn(emptyResponse()).when(surreal).query(contains("COUNT()"));
        doThrow(new RuntimeException("db down")).when(surreal).query(contains("ORDER BY message.code ASC"));

        PageRequest request = PageRequest.of(0, 2);
        assertThatThrownBy(() -> adapter.findMessagesByEnvironment("env-1", request, new MessageFilter()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
        verify(log, times(2)).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void findMessagesByEnvironment_returnsEmptyListWhenDataNotArray() {
        doReturn(emptyResponse()).when(surreal).query(contains("COUNT()"));
        doReturn(emptyResponse()).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());

        assertThat(page.getData()).isEmpty();
        assertThat(page.getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_includesCodeFilter_inGeneratedQuery() {
        MessageFilter filter = MessageFilter.builder().code("hello").build();
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 10), filter);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        List<String> queries = queryCaptor.getAllValues();
        assertThat(queries).allSatisfy(q ->
                assertThat(q).contains("string::contains(string::lowercase(message.code)")
        );
    }

    @Test
    void findMessagesByEnvironment_appliesCatalogIdFiltersToBothCountAndPageQueries() {
        String statusId = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
        String functionalityId = "c9dc13ee-2f0e-4912-946d-e7e4352c6510";
        MessageFilter filter = MessageFilter.builder()
                .code("welcome")
                .statusId(statusId)
                .functionalityId(functionalityId)
                .environmentIds(List.of("env-1", "env-2"))
                .build();
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment("app-1", PageRequest.of(0, 10), filter);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues()).allSatisfy(query -> assertThat(query)
                .contains("environment_id")
                .contains("message.status.id = 'message_state:" + statusId + "'")
                .contains("message.status.id = '" + statusId + "'")
                .contains("message.status.id = message_state:`" + statusId + "`")
                .contains("message.functionality.id = 'functionality:" + functionalityId + "'")
                .contains("message.functionality.id = '" + functionalityId + "'")
                .contains("message.functionality.id = functionality:`" + functionalityId + "`")
                .contains("welcome")
                .doesNotContain("message.status.name", "message.functionality.name", "'Active'", "'Inactive'"));
    }

    @Test
    void findMessagesByEnvironment_noFilterConditions_whenFilterEmpty() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 10), new MessageFilter());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        List<String> queries = queryCaptor.getAllValues();
        assertThat(queries).allSatisfy(q ->
                assertThat(q).doesNotContain("string::contains")
        );
    }

    @Test
    void findMessagesByEnvironment_omitsFilterConditions_whenFilterIsNull() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 10), null);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues()).allSatisfy(q ->
                assertThat(q).contains("environment_id").doesNotContain("string::contains"));
    }

    @Test
    void findMessagesByEnvironment_appendsLowercasedConditionsOnlyForNonBlankFilterValues() {
        MessageFilter filter = MessageFilter.builder()
                .code("MSG")
                .title("   ")
                .category("General")
                .functionality("Login")
                .type("Text")
                .status("Active")
                .build();
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 10), filter);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues()).allSatisfy(q -> assertThat(q)
                .contains(" AND string::contains(string::lowercase(message.code), 'msg')")
                .contains(" AND string::contains(string::lowercase(message.category.name), 'general')")
                .contains(" AND string::contains(string::lowercase(message.functionality.name), 'login')")
                .contains(" AND string::contains(string::lowercase(message.type.name), 'text')")
                .contains(" AND string::contains(string::lowercase(message.status.name), 'active')")
                .doesNotContain("message.title"));
    }

    @Test
    void findMessagesByEnvironment_reportsZeroPages_whenPageSizeIsZero() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getOffset()).thenReturn(0L);
        when(pageable.getPageSize()).thenReturn(0);
        when(pageable.getPageNumber()).thenReturn(0);
        Value countValue = mock(Value.class);
        when(countValue.isString()).thenReturn(true);
        when(countValue.getString()).thenReturn("3");
        doReturn(countResponseWith(countValue)).when(surreal).query(contains("COUNT()"));
        doReturn(emptyResponse()).when(surreal).query(contains("ORDER BY message.code ASC"));

        SimplePage<MessageData> page = adapter.findMessagesByEnvironment("env-1", pageable, new MessageFilter());

        assertThat(page.getTotalItems()).isEqualTo(3);
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountResponseIsNull() {
        doReturn(null).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountStatementIsNull() {
        doReturn(countResponseWithStatement(null)).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountStatementIsNotArray() {
        doReturn(countResponseWithStatement(mock(Value.class))).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountArrayIsEmpty() {
        Array array = mock(Array.class);
        when(array.len()).thenReturn(0);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(array);
        doReturn(countResponseWithStatement(statement)).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenFirstCountRowIsNull() {
        doReturn(countResponseWithFirstRow(null)).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenFirstCountRowIsNotObject() {
        doReturn(countResponseWithFirstRow(mock(Value.class))).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountFieldIsMissing() {
        doReturn(countResponseWith(null)).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_returnsZeroCount_whenCountFieldIsNullValue() {
        Value countValue = mock(Value.class);
        when(countValue.isNull()).thenReturn(true);
        doReturn(countResponseWith(countValue)).when(surreal).query(contains("COUNT()"));

        assertThat(findPageWithEmptyData().getTotalItems()).isZero();
    }

    @Test
    void findMessagesByEnvironment_logsAndReturnsZeroCount_whenCountQueryFails() {
        RuntimeException countError = new RuntimeException("count down");
        doThrow(countError).when(surreal).query(contains("COUNT()"));

        SimplePage<MessageData> page = findPageWithEmptyData();

        assertThat(page.getTotalItems()).isZero();
        assertThat(page.getData()).isEmpty();
        verify(log).error("msg", countError);
    }

    @Test
    void findById_fallsBackToDefaults_whenCatalogValuesAreMissingNullNoneOrScalar() {
        String uuid = UUID.randomUUID().toString();
        Value nullValue = mock(Value.class);
        when(nullValue.isNull()).thenReturn(true);
        Value noneValue = mock(Value.class);
        when(noneValue.isNone()).thenReturn(true);
        Value noneCreatedAt = mock(Value.class);
        when(noneCreatedAt.isNone()).thenReturn(true);

        Object doc = mock(Object.class);
        doReturn(mock(Value.class)).when(doc).get("message");
        doReturn(recordIdValue("message", uuid)).when(doc).get("id");
        doReturn(stringValue("CODE-9")).when(doc).get("code");
        doReturn(stringValue("Title")).when(doc).get("title");
        doReturn(stringValue("Content")).when(doc).get("content");
        doReturn(stringValue("App")).when(doc).get("application");
        doReturn(noneCreatedAt).when(doc).get("created_at");
        doReturn(null).when(doc).get("updated_at");
        doReturn(null).when(doc).get("type");
        doReturn(nullValue).when(doc).get("category");
        doReturn(noneValue).when(doc).get("status");
        doReturn(stringValue("Login")).when(doc).get("functionality");
        doReturn(responseWithOne(doc)).when(surreal).query(anyString());

        MessageData message = adapter.findById(uuid).orElseThrow();

        assertThat(message.getCode()).isEqualTo("CODE-9");
        assertThat(message.getCreatedAt()).isNull();
        assertThat(message.getType().getId()).isEqualTo(DEFAULT_UUID);
        assertThat(message.getType().getName()).isEmpty();
        assertThat(message.getCategory().getId()).isEqualTo(DEFAULT_UUID);
        assertThat(message.getCategory().getName()).isEmpty();
        assertThat(message.getStatus().getId()).isEqualTo(DEFAULT_UUID);
        assertThat(message.getStatus().getName()).isEmpty();
        assertThat(message.getFunctionality().getId()).isEqualTo(DEFAULT_UUID);
        assertThat(message.getFunctionality().getName()).isEqualTo("Login");
    }

    @Test
    void findById_convertsSurrealDateTimeCreatedAt_toInstant() {
        String uuid = UUID.randomUUID().toString();
        Value timestamp = mock(Value.class);
        when(timestamp.isDateTime()).thenReturn(true);
        when(timestamp.getDateTime()).thenReturn(ZonedDateTime.parse("2025-01-02T03:04:05Z"));
        doReturn(responseWithOne(messageDocument(uuid, "CODE-1", timestamp)))
                .when(surreal).query(anyString());

        MessageData message = adapter.findById(uuid).orElseThrow();

        assertThat(message.getCreatedAt()).isEqualTo(java.time.Instant.parse("2025-01-02T03:04:05Z"));
    }

    private SimplePage<MessageData> findPageWithEmptyData() {
        doReturn(emptyResponse()).when(surreal).query(contains("ORDER BY message.code ASC"));
        return adapter.findMessagesByEnvironment("env-1", PageRequest.of(0, 2), new MessageFilter());
    }

    private Response countResponseWithStatement(Value statement) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private Response countResponseWithFirstRow(Value firstRow) {
        Array array = mock(Array.class);
        when(array.len()).thenReturn(1);
        when(array.get(0)).thenReturn(firstRow);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(array);
        return countResponseWithStatement(statement);
    }

    private Response countResponseWith(Value countValue) {
        Object countObj = mock(Object.class);
        when(countObj.get("count")).thenReturn(countValue);
        Value firstRow = mock(Value.class);
        when(firstRow.isObject()).thenReturn(true);
        when(firstRow.getObject()).thenReturn(countObj);
        return countResponseWithFirstRow(firstRow);
    }

    private Response emptyResponse() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }
}
