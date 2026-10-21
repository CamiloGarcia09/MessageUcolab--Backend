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
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Surreal;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMessageSurrealAdapterTest {

    private static final UUID MESSAGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175701");
    private static final UUID TYPE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175702");
    private static final UUID CATEGORY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175703");
    private static final UUID STATUS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175704");
    private static final UUID APP_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175705");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175706");
    private static final String ENVIRONMENT_ID = "123e4567-e89b-12d3-a456-426614175707";
    private static final String PRODUCTION_ID = "123e4567-e89b-12d3-a456-426614175709";
    private static final String MESSAGE_ENVIRONMENT_STATE_ID = "123e4567-e89b-12d3-a456-426614175708";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private CreateMessageSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CreateMessageSurrealAdapter.class)).thenReturn(log);
        CatalogPortStaticRef.set(catalogPort);
        adapter = new CreateMessageSurrealAdapter(surreal, loggerFactory);
    }

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    private MessageData buildMessage() {
        MessageData message = MessageData.build();
        message.setId(MESSAGE_ID);
        message.setCode("MSG-001");
        message.setTitle("A valid title");
        message.setContent("A valid content");

        message.setType(new MessageTypeData(TYPE_ID, "TEXT"));
        message.setCategory(new MessageCategoryData(CATEGORY_ID, "GENERAL"));
        message.setStatus(new StatusMessageData(STATUS_ID, "ACTIVE"));

        ApplicationData application = ApplicationData.build(APP_ID, "App");
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setId(FUNCTIONALITY_ID);
        functionality.setName("Search");
        functionality.setApplication(application);
        message.setFunctionality(functionality);
        return message;
    }

    @Test
    void createMessage_executesMessageAndEnvironmentUpsertsInsideASingleTransaction() {
        MessageData message = buildMessage();

        adapter.createMessage(message, List.of(ENVIRONMENT_ID), MESSAGE_ENVIRONMENT_STATE_ID);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(1)).query(queries.capture());
        String transaction = queries.getValue();

        String expectedMessageUpsert = "UPSERT message:`" + MESSAGE_ID + "` CONTENT { "
                + "code: 'MSG-001', "
                + "title: 'A valid title', "
                + "content: 'A valid content', "
                + "type_id: message_type:`" + TYPE_ID + "`, "
                + "category_id: message_category:`" + CATEGORY_ID + "`, "
                + "status_id: message_state:`" + STATUS_ID + "`, "
                + "application_id: application:`" + APP_ID + "`, "
                + "functionality_id: functionality:`" + FUNCTIONALITY_ID + "` };";
        int messageUpsertIndex = transaction.indexOf("UPSERT message:`");
        int environmentUpsertIndex = transaction.indexOf("UPSERT message_environment:`");

        assertAll(
                () -> assertThat(transaction).startsWith("BEGIN TRANSACTION;"),
                () -> assertThat(transaction).endsWith("COMMIT TRANSACTION;"),
                () -> assertThat(transaction).contains(expectedMessageUpsert),
                () -> assertThat(messageUpsertIndex).isPositive(),
                () -> assertThat(environmentUpsertIndex).isGreaterThan(messageUpsertIndex),
                () -> assertThat(transaction)
                        .contains("message_id: message:`" + MESSAGE_ID + "`")
                        .contains("environment_id: environment:`" + ENVIRONMENT_ID + "`")
                        .contains("state_data_id: message_environment_state:`"
                                + MESSAGE_ENVIRONMENT_STATE_ID + "`"));
        verify(log).info(
                "Creating message and environment relation atomically for message: {}",
                MESSAGE_ID.toString());
    }

    @Test
    void createMessage_stripsTablePrefix_whenIdsArriveAsRecordIds() {
        MessageData message = buildMessage();

        adapter.createMessage(message, List.of("environment:" + ENVIRONMENT_ID),
                "message_environment_state:" + MESSAGE_ENVIRONMENT_STATE_ID);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(1)).query(queries.capture());
        String transaction = queries.getValue();

        assertAll(
                () -> assertThat(transaction).contains(
                        "environment_id: environment:`" + ENVIRONMENT_ID + "`"),
                () -> assertThat(transaction).contains(
                        "state_data_id: message_environment_state:`"
                                + MESSAGE_ENVIRONMENT_STATE_ID + "`"),
                () -> assertThat(transaction).doesNotContain("environment:`environment:"),
                () -> assertThat(transaction)
                        .doesNotContain("message_environment_state:`message_environment_state:"));
    }

    @Test
    void createMessage_throwsBusinessException_whenQueryFails() {
        when(catalogPort.getMessage(anyString())).thenReturn("msg");
        doThrow(new RuntimeException("db down")).when(surreal).query(anyString());
        MessageData message = buildMessage();

        assertThatThrownBy(() -> adapter.createMessage(message, List.of(ENVIRONMENT_ID),
                MESSAGE_ENVIRONMENT_STATE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getTechnicalMessage())
                        .isEqualTo("msg"));
        verify(log).error(anyString(), any(RuntimeException.class));
    }

    @Test
    void createMessage_linksTheSameMessageToEverySelectedEnvironmentInOneTransaction() {
        MessageData message = buildMessage();

        adapter.createMessage(message, List.of(ENVIRONMENT_ID, PRODUCTION_ID), MESSAGE_ENVIRONMENT_STATE_ID);

        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(1)).query(queries.capture());
        String transaction = queries.getValue();
        assertAll(
                () -> assertThat(transaction.split("UPSERT message:`", -1)).hasSize(2),
                () -> assertThat(transaction.split("UPSERT message_environment:`", -1)).hasSize(3),
                () -> assertThat(transaction).contains("environment_id: environment:`" + ENVIRONMENT_ID + "`"),
                () -> assertThat(transaction).contains("environment_id: environment:`" + PRODUCTION_ID + "`"),
                () -> assertThat(transaction).startsWith("BEGIN TRANSACTION;"),
                () -> assertThat(transaction).endsWith("COMMIT TRANSACTION;"));
    }
}
