package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.infraestructure.secondaryadapters.repository.data.OrganizationSurrealMapper;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSurrealRepositoryAdapterImplTest {

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private OrganizationSurrealRepositoryAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(OrganizationSurrealRepositoryAdapterImpl.class)).thenReturn(log);
        adapter = new OrganizationSurrealRepositoryAdapterImpl(
                surreal, loggerFactory, new OrganizationSurrealMapper());
    }

    @Test
    void findById_returnsOrganization_whenDocumentExists() {
        UUID id = UUID.randomUUID();
        doReturn(responseWithOne(organizationDocument(id, "UCO"))).when(surreal).query(anyString());

        Optional<OrganizationEntity> result = adapter.findById(id);

        assertThat(result).hasValueSatisfying(organization -> assertThat(organization)
                .extracting(OrganizationEntity::getId, OrganizationEntity::getName)
                .containsExactly(id, "UCO"));
        verifyQuery("SELECT * FROM organization:`" + id + "` LIMIT 1;");
    }

    @Test
    void findByName_returnsOrganization_andEscapesName() {
        UUID id = UUID.randomUUID();
        doReturn(responseWithOne(organizationDocument(id, "UCO's"))).when(surreal).query(anyString());

        Optional<OrganizationEntity> result = adapter.findByName("UCO's");

        assertThat(result).hasValueSatisfying(organization ->
                assertThat(organization.getName()).isEqualTo("UCO's"));
        verifyQuery("SELECT * FROM organization WHERE name = 'UCO\\'s' LIMIT 1;");
    }

    @Test
    void findByName_returnsEmpty_whenDocumentDoesNotExist() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        Optional<OrganizationEntity> result = adapter.findByName("UCO");

        assertThat(result).isEmpty();
    }

    @Test
    void create_persistsMappedOrganization_withoutLoggingBusinessData() {
        UUID id = UUID.randomUUID();
        OrganizationEntity organization = organization(id, "UCO");

        adapter.create(organization);

        verifyQuery("UPSERT organization:`" + id + "` CONTENT { name: 'UCO' };");
        verify(log).info("TCH_103");
    }

    @Test
    void create_throwsBusinessException_whenQueryFails() {
        RuntimeException cause = new RuntimeException("db down");
        OrganizationEntity organization = organization(UUID.randomUUID(), "UCO");
        doThrow(cause).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.create(organization))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getTechnicalMessage())
                        .isEqualTo("Error al persistir la organizacion en la base de datos SurrealDB"));
        verify(log).error("Error al persistir la organizacion en SurrealDB", cause);
    }

    private OrganizationEntity organization(final UUID id, final String name) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(id);
        organization.setName(name);
        return organization;
    }

    private Object organizationDocument(final UUID id, final String name) {
        Object document = mock(Object.class);
        doReturn(recordIdValue(id)).when(document).get("id");
        doReturn(stringValue(name)).when(document).get("name");
        return document;
    }

    private Value recordIdValue(final UUID id) {
        RecordId recordId = mock(RecordId.class);
        when(recordId.toString()).thenReturn("organization:" + id);
        Value value = mock(Value.class);
        when(value.isRecordId()).thenReturn(true);
        when(value.getRecordId()).thenReturn(recordId);
        return value;
    }

    private Value stringValue(final String text) {
        Value value = mock(Value.class);
        when(value.isNull()).thenReturn(false);
        when(value.isNone()).thenReturn(false);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(text);
        return value;
    }

    private Response responseWithOne(final Object document) {
        Response response = mock(Response.class);
        Value statement = mock(Value.class);
        Array array = mock(Array.class);
        Value item = mock(Value.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(array);
        when(array.len()).thenReturn(1);
        when(array.get(0)).thenReturn(item);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(document);
        return response;
    }

    private Response emptyResponse() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        return response;
    }

    private void verifyQuery(final String expectedSql) {
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(expectedSql);
    }
}
