package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import com.surrealdb.Array;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvironmentSurrealRepositoryAdapterImplTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private EnvironmentSurrealRepositoryAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(EnvironmentSurrealRepositoryAdapterImpl.class)).thenReturn(log);
        lenient().when(catalogPort.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("msg");
        lenient().when(catalogPort.getMessage("TCH_101"))
                .thenReturn("Ejecutando el guardado del ambiente mediante SurrealQL: {}");
        CatalogPortStaticRef.set(catalogPort);
        adapter = new EnvironmentSurrealRepositoryAdapterImpl(surreal, loggerFactory);
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

    private Value recordIdValue(String table, String uuid) {
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn(table + ":" + uuid);
        Value v = mock(Value.class);
        when(v.isRecordId()).thenReturn(true);
        when(v.getRecordId()).thenReturn(recordId);
        return v;
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

    private Object environmentDocument(String envUuid, String appUuid, String typeId, String typeName) {
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", envUuid)).when(doc).get("id");
        doReturn(recordIdValue("application", appUuid)).when(doc).get("application_id");
        doReturn(recordIdValue("environment_type", typeId)).when(doc).get("type_id");
        doReturn(stringValue(typeName)).when(doc).get("type_name");
        return doc;
    }

    private String capturedQuery() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(captor.capture());
        return captor.getValue();
    }

    @Test
    void findById_returnsEnvironmentWithTypeName_whenFound() {
        String envUuid = UUID.randomUUID().toString();
        String appUuid = UUID.randomUUID().toString();
        String typeId = UUID.randomUUID().toString();
        doReturn(responseWithOne(environmentDocument(envUuid, appUuid, typeId, "PROD")))
                .when(surreal).query(anyString());

        Optional<EnvironmentData> result = adapter.findById(envUuid);

        assertThat(result).hasValueSatisfying(environment -> assertThat(environment)
                .extracting(
                        EnvironmentData::getId,
                        value -> value.getApplication().getId(),
                        value -> value.getType().getId(),
                        value -> value.getType().getName())
                .containsExactly(UUID.fromString(envUuid), UUID.fromString(appUuid),
                        UUID.fromString(typeId), "PROD"));
        assertThat(capturedQuery()).contains("type_id.name AS type_name");
    }

    @Test
    void findById_returnsEnvironmentWithNullApplicationId() {
        String envUuid = UUID.randomUUID().toString();
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", envUuid)).when(doc).get("id");
        doReturn(recordIdValue("environment_type", UUID.randomUUID().toString())).when(doc).get("type_id");
        doReturn(stringValue("DEV")).when(doc).get("type_name");
        when(doc.get("application_id")).thenReturn(null);
        doReturn(responseWithOne(doc)).when(surreal).query(anyString());

        Optional<EnvironmentData> result = adapter.findById(envUuid);

        assertThat(result).hasValueSatisfying(environment -> assertSoftly(softly -> {
            softly.assertThat(environment.getType().getName()).isEqualTo("DEV");
            softly.assertThat(environment.getApplication()).isNotNull();
        }));
    }

    @Test
    void findById_keepsDefaultType_whenTypeDataIsMissing() {
        String envUuid = UUID.randomUUID().toString();
        Object doc = mock(Object.class);
        doReturn(recordIdValue("environment", envUuid)).when(doc).get("id");
        doReturn(recordIdValue("application", UUID.randomUUID().toString())).when(doc).get("application_id");
        doReturn(null).when(doc).get("type_id");
        doReturn(responseWithOne(doc)).when(surreal).query(anyString());

        Optional<EnvironmentData> result = adapter.findById(envUuid);

        assertThat(result).hasValueSatisfying(environment -> assertThat(environment.getType().getName()).isEmpty());
    }

    @Test
    void findById_returnsEmpty_whenResponseNull() {
        when(surreal.query(anyString())).thenReturn(null);

        assertThat(adapter.findById("env-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenResponseSizeZero() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.findById("env-1")).isEmpty();
    }

    @Test
    void findById_returnsEmpty_whenStatementNotArray() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(false);
        when(response.take(0)).thenReturn(statement);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findById("env-1")).isEmpty();
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

        assertThat(adapter.findById("env-1")).isEmpty();
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

        assertThat(adapter.findById("env-1")).isEmpty();
    }

    @Test
    void findById_wrapsUnexpectedFailureAsTechnicalBusinessException_whenQueryFails() {
        RuntimeException failure = new RuntimeException("boom");
        when(surreal.query(anyString())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.findById("env-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException technical = (BusinessException) exception;
                    assertAll(
                            () -> assertThat(technical.getType()).isEqualTo(ExceptionType.TECHNICAL),
                            () -> assertThat(technical.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE),
                            () -> assertThat(technical.getTechnicalMessage())
                                    .isEqualTo("Error al consultar el entorno en la base de datos SurrealDB"),
                            () -> assertThat(technical.getRootException()).isSameAs(failure));
                });
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void findById_keepsBusinessExceptionUnwrapped_whenQueryFailsWithDomainException() {
        BusinessException failure = BusinessException.buildTechnicalException(
                "environment read unavailable", new IllegalStateException("driver failure"),
                ExceptionLocation.INFRASTRUCTURE);
        when(surreal.query(anyString())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.findById("env-1")).isSameAs(failure);
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void existsByApplicationIdAndTypeId_returnsTrue_whenEnvironmentAlreadyExists() {
        doReturn(responseWithOne(mock(Object.class))).when(surreal).query(anyString());

        boolean result = adapter.existsByApplicationIdAndTypeId("app-1", "type-1");

        assertThat(result).isTrue();
        assertThat(capturedQuery())
                .contains("application_id = application:`app-1`")
                .contains("type_id = environment_type:`type-1`");
    }

    @Test
    void existsByApplicationIdAndTypeId_returnsFalse_whenNoEnvironmentMatches() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.existsByApplicationIdAndTypeId("app-1", "type-1")).isFalse();
    }

    @Test
    void existsByApplicationIdAndTypeId_wrapsUnexpectedFailureAsTechnicalBusinessException_whenQueryFails() {
        RuntimeException failure = new RuntimeException("connection lost");
        when(surreal.query(anyString())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.existsByApplicationIdAndTypeId("app-1", "type-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException technical = (BusinessException) exception;
                    assertAll(
                            () -> assertThat(technical.getType()).isEqualTo(ExceptionType.TECHNICAL),
                            () -> assertThat(technical.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE),
                            () -> assertThat(technical.getTechnicalMessage())
                                    .isEqualTo("Error al validar el entorno en la base de datos SurrealDB"),
                            () -> assertThat(technical.getRootException()).isSameAs(failure));
                });
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void existsByApplicationIdAndTypeId_keepsBusinessExceptionUnwrapped_whenQueryFailsWithDomainException() {
        BusinessException failure = BusinessException.buildTechnicalException(
                "environment existence check unavailable", new IllegalStateException("driver failure"),
                ExceptionLocation.INFRASTRUCTURE);
        when(surreal.query(anyString())).thenThrow(failure);

        assertThatThrownBy(() -> adapter.existsByApplicationIdAndTypeId("app-1", "type-1"))
                .isSameAs(failure);
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void create_persistsEnvironmentReferencesWithoutDenormalizedName() {
        UUID environmentId = UUID.fromString("123e4567-e89b-12d3-a456-426614175010");
        UUID applicationId = UUID.fromString("123e4567-e89b-12d3-a456-426614175011");
        UUID typeId = UUID.fromString("123e4567-e89b-12d3-a456-426614175012");
        EnvironmentData environment = new EnvironmentData(environmentId,
                ApplicationData.build(applicationId, "Application"),
                new EnvironmentTypeData(typeId, "Production"));
        String expectedQuery = "UPSERT environment:`" + environmentId + "` CONTENT { "
                + "application_id: application:`" + applicationId + "`, "
                + "type_id: environment_type:`" + typeId + "`, "
                + "state_id: environment_state:`state-1` };";

        adapter.create(environment, typeId.toString(), "state-1");

        verify(surreal).query(expectedQuery);
        verify(log).info("Ejecutando el guardado del ambiente mediante SurrealQL: {}", expectedQuery);
    }

    @Test
    void create_wrapsDriverFailureAsTechnicalBusinessException() {
        EnvironmentData environment = new EnvironmentData(UUID.randomUUID(),
                ApplicationData.build(UUID.randomUUID(), "Application"), EnvironmentTypeData.build());
        RuntimeException failure = new RuntimeException("db down");
        doThrow(failure).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.create(environment, "type-1", "state-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getTechnicalMessage())
                        .isEqualTo("Error al persistir el entorno en la base de datos SurrealDB"));
        verify(log).error("Error al persistir el entorno en SurrealDB", failure);
    }
}
