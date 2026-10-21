package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import com.surrealdb.Array;
import com.surrealdb.Object;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Consultas por aplicacion con filtro de ambientes y ambientes de cada mensaje.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageSurrealApplicationQueriesTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String DEVELOP_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final String PRODUCTION_ID = "de1b8223-1e9c-4807-beec-b2c1cff44e73";
    private static final String MESSAGE_ID = "5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164";

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
        when(catalogPort.getMessage(anyString())).thenReturn("%s");
        CatalogPortStaticRef.set(catalogPort);
        adapter = new MessageSurrealRepositoryAdapterImpl(surreal, loggerFactory);
        // Las respuestas se construyen antes de stubbear: crear mocks dentro de when() deja el stub incompleto.
        Response environments = rows(
                Map.of("id", text("environment:`" + DEVELOP_ID + "`"), "name", text("Develop")),
                Map.of("id", text("environment:`" + PRODUCTION_ID + "`"), "name", text("Production")));
        Response relations = rows(
                Map.of("code", text("MSG_A"), "environment_id", text("environment:`" + DEVELOP_ID + "`")),
                Map.of("code", text("MSG_A"), "environment_id", text("environment:`" + PRODUCTION_ID + "`")),
                Map.of("code", text("MSG_B"), "environment_id", text("environment:`" + DEVELOP_ID + "`")));
        when(surreal.query(startsWith("SELECT id, type_id.name AS name FROM environment"))).thenReturn(environments);
        when(surreal.query(startsWith("SELECT message.code AS code, environment_id FROM message_environment_readmodel")))
                .thenReturn(relations);
    }

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void findMessagesByApplication_listsApplicationMessagesWithTheirEnvironments() {
        Response total = count(1);
        when(surreal.query(startsWith("SELECT count() AS count"))).thenReturn(total);
        Response documents = rows(document("MSG_A"));
        when(surreal.query(startsWith("SELECT * FROM message_data_collection"))).thenReturn(documents);

        SimplePage<MessageData> page = adapter.findMessagesByApplication(APPLICATION_ID, List.of(),
                PageRequest.of(0, 20), new MessageFilter());

        assertThat(page.getTotalItems()).isEqualTo(1);
        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getEnvironments())
                .extracting(environment -> environment.getName())
                .containsExactly("Develop", "Production");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, atLeastOnce()).query(queries.capture());
        assertThat(queries.getAllValues())
                .anySatisfy(sql -> assertThat(sql).startsWith("SELECT * FROM message_data_collection")
                        .contains("string::contains(<string> application_id, '" + APPLICATION_ID + "')")
                        .doesNotContain("code IN"));
    }

    @Test
    void findMessagesByApplication_restrictsToMessagesOfSelectedEnvironments() {
        Response total = count(1);
        when(surreal.query(startsWith("SELECT count() AS count"))).thenReturn(total);
        Response documents = rows(document("MSG_A"));
        when(surreal.query(startsWith("SELECT * FROM message_data_collection"))).thenReturn(documents);

        adapter.findMessagesByApplication(APPLICATION_ID, List.of(PRODUCTION_ID), PageRequest.of(0, 20),
                new MessageFilter());

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, atLeastOnce()).query(queries.capture());
        assertThat(queries.getAllValues())
                .anySatisfy(sql -> assertThat(sql).startsWith("SELECT * FROM message_data_collection")
                        .contains("code IN ['MSG_A']"));
    }

    @Test
    void findMessagesByApplication_returnsEmptyPageWithoutQueryingMessages_whenNoMessageIsInSelectedEnvironments() {
        SimplePage<MessageData> page = adapter.findMessagesByApplication(APPLICATION_ID,
                List.of("123e4567-e89b-12d3-a456-426614175999"), PageRequest.of(0, 20), new MessageFilter());

        assertThat(page.getData()).isEmpty();
        assertThat(page.getTotalItems()).isZero();
        verify(surreal, never()).query(contains("message_data_collection"));
    }

    @Test
    void findByCodeAndApplication_returnsMessageWithItsEnvironments() {
        Response documents = rows(document("MSG_B"));
        when(surreal.query(startsWith("SELECT * FROM message_data_collection"))).thenReturn(documents);

        Optional<MessageData> message = adapter.findByCodeAndApplication("MSG_B", APPLICATION_ID);

        assertThat(message).isPresent();
        assertThat(message.get().getEnvironments())
                .extracting(environment -> environment.getName())
                .containsExactly("Develop");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, atLeastOnce()).query(queries.capture());
        assertThat(queries.getAllValues())
                .anySatisfy(sql -> assertThat(sql).contains("WHERE code = 'MSG_B'")
                        .contains("string::contains(<string> application_id, '" + APPLICATION_ID + "')"));
    }

    private Map<String, Value> document(String code) {
        return Map.of(
                "id", text("message_data_collection:`" + MESSAGE_ID + "`"),
                "code", text(code),
                "title", text("A title"),
                "content", text("A content"),
                "application", text("MessageUcoLab"));
    }

    @SafeVarargs
    private static Response rows(Map<String, Value>... rows) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(rows.length);
        for (int i = 0; i < rows.length; i++) {
            Object row = mock(Object.class);
            rows[i].forEach((field, value) -> when(row.get(field)).thenReturn(value));
            Value item = mock(Value.class);
            when(item.isObject()).thenReturn(true);
            when(item.getObject()).thenReturn(row);
            when(array.get(i)).thenReturn(item);
        }
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private static Response count(long total) {
        return rows(Map.of("count", text(String.valueOf(total))));
    }

    private static Value text(String value) {
        Value v = mock(Value.class);
        when(v.isString()).thenReturn(true);
        when(v.getString()).thenReturn(value);
        return v;
    }
}
