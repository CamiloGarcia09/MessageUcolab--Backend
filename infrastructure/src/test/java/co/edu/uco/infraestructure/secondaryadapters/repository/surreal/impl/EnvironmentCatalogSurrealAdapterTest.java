package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvironmentCatalogSurrealAdapterTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private EnvironmentCatalogSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(EnvironmentCatalogSurrealAdapter.class)).thenReturn(log);
        adapter = new EnvironmentCatalogSurrealAdapter(surreal, loggerFactory);
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

    private Object environmentDocument(String uuid, String appId, String typeId, String typeName) {
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", uuid)).when(doc).get("id");
        doReturn(recordIdValue("application", appId)).when(doc).get("application_id");
        doReturn(recordIdValue("environment_type", typeId)).when(doc).get("type_id");
        doReturn(stringValue(typeName)).when(doc).get("type_name");
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
    void findAllByApplicationId_mapsEnvironmentsWithApplicationIdAndTypeName() {
        String uuid = UUID.randomUUID().toString();
        String appId = UUID.randomUUID().toString();
        String typeId = UUID.randomUUID().toString();
        doReturn(responseWithOne(environmentDocument(uuid, appId, typeId, "Prod")))
                .when(surreal).query(anyString());

        List<EnvironmentData> result = adapter.findAllByApplicationId(appId);

        assertThat(result).singleElement().satisfies(environment -> assertThat(environment)
                .extracting(
                        EnvironmentData::getId,
                        value -> value.getApplication().getId(),
                        value -> value.getType().getId(),
                        value -> value.getType().getName())
                .containsExactly(UUID.fromString(uuid), UUID.fromString(appId), UUID.fromString(typeId), "Prod"));
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(queryCaptor.capture());
        assertThat(queryCaptor.getValue())
                .contains("type_id.name AS type_name")
                .contains("application_id = application:`" + appId + "`");
    }

    @Test
    void findAllByApplicationId_mapsEnvironmentWithoutApplicationId() {
        String uuid = UUID.randomUUID().toString();
        String typeId = UUID.randomUUID().toString();
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", uuid)).when(doc).get("id");
        doReturn(recordIdValue("environment_type", typeId)).when(doc).get("type_id");
        doReturn(stringValue("Dev")).when(doc).get("type_name");
        doReturn(null).when(doc).get("application_id");
        doReturn(responseWithOne(doc)).when(surreal).query(anyString());

        List<EnvironmentData> result = adapter.findAllByApplicationId("app-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplication()).isNotNull();
        assertThat(result.get(0).getType().getName()).isEqualTo("Dev");
    }

    @Test
    void findAllByApplicationId_keepsDefaultType_whenTypeDataIsMissing() {
        String uuid = UUID.randomUUID().toString();
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", uuid)).when(doc).get("id");
        doReturn(recordIdValue("application", UUID.randomUUID().toString())).when(doc).get("application_id");
        doReturn(null).when(doc).get("type_id");
        doReturn(responseWithOne(doc)).when(surreal).query(anyString());

        List<EnvironmentData> result = adapter.findAllByApplicationId("app-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isNotNull();
        assertThat(result.get(0).getType().getName()).isEmpty();
    }

    @Test
    void findAllByApplicationId_returnsEmptyList_whenResponseEmpty() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findAllByApplicationId("app-1")).isEmpty();
    }
}
