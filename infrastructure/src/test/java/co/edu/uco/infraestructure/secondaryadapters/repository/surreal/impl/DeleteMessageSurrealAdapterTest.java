package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteMessageSurrealAdapterTest {

    private static final String MESSAGE_ID = "123e4567-e89b-12d3-a456-426614175901";
    private static final String MESSAGE_RECORD = "message:`" + MESSAGE_ID + "`";
    private static final String LOG_MESSAGE = "delete failed (log)";
    private static final String TECHNICAL_MESSAGE = "delete failed (technical)";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private CatalogPort catalogPort;

    private DeleteMessageSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(DeleteMessageSurrealAdapter.class)).thenReturn(log);
        lenient().when(catalogPort.getMessage("TCH_100"))
                .thenReturn("Eliminando el mensaje y sus relaciones con ambientes de forma atómica: {}");
        CatalogPortStaticRef.set(catalogPort);
        adapter = new DeleteMessageSurrealAdapter(surreal, loggerFactory);
    }

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    private String captureSingleQuery() {
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(surreal, times(1)).query(queries.capture());
        return queries.getValue();
    }

    @Test
    void deleteMessage_removesEnvironmentRelationsBeforeMessageInsideASingleTransaction() {
        adapter.deleteMessage(MESSAGE_ID);

        String expectedSql = "BEGIN TRANSACTION;"
                + "DELETE message_environment WHERE message_id = " + MESSAGE_RECORD + ";"
                + "DELETE " + MESSAGE_RECORD + ";"
                + "COMMIT TRANSACTION;";
        assertThat(captureSingleQuery()).isEqualTo(expectedSql);
        verify(log).info("Eliminando el mensaje y sus relaciones con ambientes de forma atómica: {}", MESSAGE_ID);
    }

    @Test
    void deleteMessage_stripsTablePrefixAndWhitespace_whenIdArrivesAsRecordId() {
        adapter.deleteMessage("  message:" + MESSAGE_ID + "  ");

        String transaction = captureSingleQuery();

        assertAll(
                () -> assertThat(transaction).contains("WHERE message_id = " + MESSAGE_RECORD + ";"),
                () -> assertThat(transaction).contains("DELETE " + MESSAGE_RECORD + ";"),
                () -> assertThat(transaction).doesNotContain("message:`message:"));
    }

    @Test
    void deleteMessage_throwsTechnicalBusinessException_whenQueryFails() {
        RuntimeException dbError = new RuntimeException("db down");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_074.getCode())).thenReturn(LOG_MESSAGE);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_075.getCode())).thenReturn(TECHNICAL_MESSAGE);
        doThrow(dbError).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.deleteMessage(MESSAGE_ID))
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
