package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageSurrealAdapterTest {

    private static final UUID MESSAGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175801");
    private static final UUID TYPE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175802");
    private static final UUID CATEGORY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175803");
    private static final UUID STATUS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175804");
    private static final UUID APP_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175805");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175806");
    private static final String DEVELOP_ID = "123e4567-e89b-12d3-a456-426614175811";
    private static final String TESTING_ID = "123e4567-e89b-12d3-a456-426614175812";
    private static final String PRODUCTION_ID = "123e4567-e89b-12d3-a456-426614175813";
    private static final String RELATION_DEVELOP = "123e4567-e89b-12d3-a456-426614175821";
    private static final String RELATION_TESTING = "123e4567-e89b-12d3-a456-426614175822";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175830";
    private static final String LOG_MESSAGE = "update failed (log)";
    private static final String TECHNICAL_MESSAGE = "update failed (technical)";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private UpdateMessageSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(UpdateMessageSurrealAdapter.class)).thenReturn(log);
        lenient().when(catalogPort.getMessage("TCH_095"))
                .thenReturn("Mensaje actualizado correctamente con el identificador: {}");
        CatalogPortStaticRef.set(catalogPort);
        adapter = new UpdateMessageSurrealAdapter(surreal, loggerFactory);
    }

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    private MessageData buildMessage() {
        MessageData message = MessageData.build();
        message.setId(MESSAGE_ID);
        message.setCode("MSG-001");
        message.setTitle("Updated title");
        message.setContent("Updated content");

        message.setType(new MessageTypeData(TYPE_ID, "TEXT"));
        message.setCategory(new MessageCategoryData(CATEGORY_ID, "GENERAL"));
        message.setStatus(new StatusMessageData(STATUS_ID, "ACTIVE"));

        FunctionalityData functionality = FunctionalityData.build();
        functionality.setId(FUNCTIONALITY_ID);
        functionality.setName("Search");
        functionality.setApplication(ApplicationData.build(APP_ID, "App"));
        message.setFunctionality(functionality);
        return message;
    }

    @Test
    void updateMessage_mergesEditableFieldsIntoTheMessageRecord() {
        adapter.updateMessage(buildMessage(), null, null);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(1)).query(queries.capture());

        String expectedSql = "UPDATE message:`" + MESSAGE_ID + "` MERGE { "
                + "title: 'Updated title', "
                + "content: 'Updated content', "
                + "type_id: message_type:`" + TYPE_ID + "`, "
                + "category_id: message_category:`" + CATEGORY_ID + "`, "
                + "status_id: message_state:`" + STATUS_ID + "`, "
                + "functionality_id: functionality:`" + FUNCTIONALITY_ID + "` };";
        assertThat(queries.getValue()).isEqualTo(expectedSql);
        verify(log).info("Mensaje actualizado correctamente con el identificador: {}", MESSAGE_ID.toString());
    }

    @Test
    void updateMessage_doesNotOverwriteCodeOrApplication() {
        adapter.updateMessage(buildMessage(), null, null);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(queries.capture());

        assertAll(
                () -> assertThat(queries.getValue()).doesNotContain("code:"),
                () -> assertThat(queries.getValue()).doesNotContain("application_id"));
    }

    @Test
    void updateMessage_throwsTechnicalBusinessException_whenQueryFails() {
        RuntimeException dbError = new RuntimeException("db down");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_072.getCode())).thenReturn(LOG_MESSAGE);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_073.getCode())).thenReturn(TECHNICAL_MESSAGE);
        doThrow(dbError).when(surreal).query(anyString());
        MessageData message = buildMessage();

        assertThatThrownBy(() -> adapter.updateMessage(message, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertAll(
                            () -> assertThat(businessException.getTechnicalMessage()).isEqualTo(TECHNICAL_MESSAGE),
                            () -> assertThat(businessException.getRootException()).isSameAs(dbError),
                            () -> assertThat(businessException.getType()).isEqualTo(ExceptionType.TECHNICAL),
                            () -> assertThat(businessException.getLocation()).isEqualTo(ExceptionLocation.INFRASTRUCTURE));
                });
        verify(log).error(LOG_MESSAGE, dbError);
    }

    @Test
    void updateMessage_keepsSelectedRelationsDeletesOthersAndCreatesMissingInOneTransaction() {
        Response current = relations(new String[] {RELATION_DEVELOP, DEVELOP_ID},
                new String[] {RELATION_TESTING, TESTING_ID});
        when(surreal.query(startsWith("SELECT"))).thenReturn(current);

        adapter.updateMessage(buildMessage(), List.of(DEVELOP_ID, PRODUCTION_ID), STATE_ID);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queries.capture());
        String transaction = queries.getAllValues().get(1);
        assertAll(
                () -> assertThat(transaction).startsWith("BEGIN TRANSACTION;UPDATE message:`" + MESSAGE_ID + "`"),
                () -> assertThat(transaction).endsWith("COMMIT TRANSACTION;"),
                () -> assertThat(transaction).contains("DELETE message_environment:`" + RELATION_TESTING + "`;"),
                () -> assertThat(transaction).doesNotContain(RELATION_DEVELOP),
                () -> assertThat(transaction).contains("environment_id: environment:`" + PRODUCTION_ID + "`"),
                () -> assertThat(transaction).doesNotContain("environment_id: environment:`" + DEVELOP_ID + "`"),
                () -> assertThat(transaction).contains("state_data_id: message_environment_state:`" + STATE_ID + "`"));
    }

    @Test
    void updateMessage_createsEveryRelation_whenMessageHasNoneYet() {
        Response empty = mock(Response.class);
        when(empty.size()).thenReturn(0);
        when(surreal.query(startsWith("SELECT"))).thenReturn(empty);

        adapter.updateMessage(buildMessage(), List.of(DEVELOP_ID, TESTING_ID), STATE_ID);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(2)).query(queries.capture());
        String transaction = queries.getAllValues().get(1);
        assertAll(
                () -> assertThat(transaction.split("UPSERT message_environment:`", -1)).hasSize(3),
                () -> assertThat(transaction).doesNotContain("DELETE"));
    }

    private Response relations(String[]... rows) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value statement = mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        Array array = mock(Array.class);
        when(array.len()).thenReturn(rows.length);
        for (int i = 0; i < rows.length; i++) {
            Object row = mock(Object.class);
            Value id = stringValue("message_environment:`" + rows[i][0] + "`");
            Value environment = stringValue("environment:`" + rows[i][1] + "`");
            when(row.get("id")).thenReturn(id);
            when(row.get("environment_id")).thenReturn(environment);
            Value item = mock(Value.class);
            when(item.isObject()).thenReturn(true);
            when(item.getObject()).thenReturn(row);
            when(array.get(i)).thenReturn(item);
        }
        when(statement.getArray()).thenReturn(array);
        when(response.take(0)).thenReturn(statement);
        return response;
    }

    private static Value stringValue(String text) {
        Value value = mock(Value.class);
        when(value.isString()).thenReturn(true);
        when(value.getString()).thenReturn(text);
        return value;
    }
}
