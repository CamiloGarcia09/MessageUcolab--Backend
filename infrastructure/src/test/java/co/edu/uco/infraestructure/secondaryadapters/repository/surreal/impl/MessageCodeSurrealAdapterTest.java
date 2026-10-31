package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageCodeSurrealAdapterTest {

    private static final String NORMALIZED_CODE = "MSG_WELCOME";
    private static final String APPLICATION_ID = "123e4567-e89b-12d3-a456-426614175709";

    @Mock
    private Surreal surreal;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private MessageCodeSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageCodeSurrealAdapter.class)).thenReturn(log);
        adapter = new MessageCodeSurrealAdapter(surreal, loggerFactory);
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

    private String capturedQuery() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(captor.capture());
        return captor.getValue();
    }

    @Test
    void existsByCodeAndApplicationId_returnsTrue_whenMessageWithCodeIsFound() {
        doReturn(responseWithOne(mock(Object.class))).when(surreal).query(anyString());

        assertThat(adapter.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID)).isTrue();
    }

    @Test
    void existsByCodeAndApplicationId_returnsFalse_whenNoMessageMatches() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        assertThat(adapter.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID)).isFalse();
    }

    @Test
    void existsByCodeAndApplicationId_filtersByQuotedCodeAndApplicationRecordId() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID);

        assertThat(capturedQuery()).isEqualTo("SELECT id FROM message WHERE code = '"
                + NORMALIZED_CODE + "' AND application_id = application:`" + APPLICATION_ID
                + "` LIMIT 1;");
    }

    @Test
    void existsByCodeAndApplicationId_escapesSingleQuotesInCode() {
        doReturn(emptyResponse()).when(surreal).query(anyString());

        adapter.existsByCodeAndApplicationId("MSG'O", APPLICATION_ID);

        assertThat(capturedQuery()).contains("code = 'MSG\\'O'");
    }

    @Test
    void existsByCodeAndApplicationId_logsAndRethrows_whenQueryFails() {
        RuntimeException cause = new RuntimeException("database unavailable");
        doThrow(cause).when(surreal).query(anyString());

        assertThatThrownBy(() -> adapter.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID))
                .isSameAs(cause);

        verify(log).error(contains("Error al validar el código del mensaje"), eq(cause));
    }
}
