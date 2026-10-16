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
import com.surrealdb.Surreal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
        adapter.updateMessage(buildMessage());

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
        verify(log).info("Updating message with id: {}", MESSAGE_ID.toString());
    }

    @Test
    void updateMessage_doesNotOverwriteCodeOrApplication() {
        adapter.updateMessage(buildMessage());

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

        assertThatThrownBy(() -> adapter.updateMessage(message))
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
}
