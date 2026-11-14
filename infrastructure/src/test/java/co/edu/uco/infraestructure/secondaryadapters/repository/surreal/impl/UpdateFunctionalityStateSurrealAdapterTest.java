package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Array;
import com.surrealdb.Object;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UpdateFunctionalityStateSurrealAdapterTest {
    private static final String FUNCTIONALITY = "123e4567-e89b-12d3-a456-426614175801";
    private static final String APPLICATION = "123e4567-e89b-12d3-a456-426614175802";
    private static final String STATE = "123e4567-e89b-12d3-a456-426614175803";
    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort logger;
    @Mock private CatalogPort catalog;
    private UpdateFunctionalityStateSurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(UpdateFunctionalityStateSurrealAdapter.class)).thenReturn(logger);
        when(catalog.getMessage(anyString())).thenReturn("operation failed");
        adapter = new UpdateFunctionalityStateSurrealAdapter(surreal, loggerFactory, catalog);
    }

    @Test
    void updateState_buildsAtomicCascadeLimitedToFunctionalityAndApplication() {
        Response response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, STATE);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(query.capture());
        assertThat(query.getValue()).contains("BEGIN TRANSACTION;")
                .contains("functionality:`" + FUNCTIONALITY + "` WHERE application_id = application:`" + APPLICATION + "`")
                .contains("LET $cascade_message_ids = (SELECT VALUE <string> id FROM message WHERE functionality_id = functionality:`" + FUNCTIONALITY + "` AND application_id = application:`" + APPLICATION + "` AND status_id != message_state:`" + STATE + "`)")
                .contains("LET $previous_message_events = (SELECT VALUE id FROM domain_events WHERE aggregate_type = 'message' AND event_type = 'MESSAGE_UPDATED'")
                .contains("UPDATE domain_events SET metadata.notification_type = 'MessagesChanged'")
                .contains("id NOT IN $previous_message_events")
                .contains("UPDATE message SET status_id = message_state:`" + STATE + "`")                .contains("functionality_id = functionality:`" + FUNCTIONALITY + "` AND application_id = application:`" + APPLICATION + "`")
                .contains("status_id != message_state:`" + STATE + "`")
                .contains("RETURN code").contains("COMMIT TRANSACTION;")
                .doesNotContain("DELETE", "UPSERT");
    }

    @Test
    void updateState_omitsCascadeWhenActivating() {
        Response response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, null);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(query.capture());
        assertThat(query.getValue()).contains("RETURN [];COMMIT TRANSACTION;")
                .doesNotContain("UPDATE message SET");
    }

    @Test
    void updateState_returnsChangedMessageCodesFromTransactionResults() {
        Response response = mock(Response.class);
        Value empty = mock(Value.class);
        when(empty.isArray()).thenReturn(true);
        Array noRows = mock(Array.class);
        when(empty.getArray()).thenReturn(noRows);
        when(noRows.len()).thenReturn(0);
        Value changedRows = codeRows("MSG-A", "MSG-B");
        when(response.size()).thenReturn(2);
        when(response.take(0)).thenReturn(empty);
        when(response.take(1)).thenReturn(changedRows);
        when(surreal.query(anyString())).thenReturn(response);

        var codes = adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, STATE);

        assertThat(codes).containsExactly("MSG-A", "MSG-B");
    }

    @Test
    void updateState_wrapsDeferredTransactionReadFailure() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(2);
        when(response.take(1)).thenThrow(new IllegalStateException("deferred transaction error"));
        when(surreal.query(anyString())).thenReturn(response);

        assertThatThrownBy(() -> adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, STATE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateState_ignoresNullNonArrayAndRowsWithoutUsableCodes() {
        Response response = mock(Response.class);
        Value notArray = mock(Value.class);
        when(notArray.isArray()).thenReturn(false);
        Value rowsResult = mock(Value.class);
        when(rowsResult.isArray()).thenReturn(true);
        Array rows = mock(Array.class);
        when(rowsResult.getArray()).thenReturn(rows);
        when(rows.len()).thenReturn(3);
        when(rows.get(0)).thenReturn(null);
        Value scalarRow = mock(Value.class);
        when(scalarRow.isObject()).thenReturn(false);
        when(rows.get(1)).thenReturn(scalarRow);
        Value objectRow = mock(Value.class);
        when(objectRow.isObject()).thenReturn(true);
        Object object = mock(Object.class);
        when(objectRow.getObject()).thenReturn(object);
        Value blankCode = mock(Value.class);
        when(blankCode.isString()).thenReturn(true);
        when(blankCode.getString()).thenReturn("  ");
        when(object.get("code")).thenReturn(blankCode);
        when(rows.get(2)).thenReturn(objectRow);
        when(response.size()).thenReturn(3);
        when(response.take(0)).thenReturn(null);
        when(response.take(1)).thenReturn(notArray);
        when(response.take(2)).thenReturn(rowsResult);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, STATE)).isEmpty();
    }

    @Test
    void updateState_wrapsDriverFailureAsTechnicalDomainException() {
        when(surreal.query(anyString())).thenThrow(new IllegalStateException("driver failure"));

        assertThatThrownBy(() -> adapter.updateState(FUNCTIONALITY, APPLICATION, STATE, STATE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void existsInApplication_returnsTrueWhenScopedRecordIsReturned() {
        Response response = rowsWithField("id", "functionality:`" + FUNCTIONALITY + "`");
        when(surreal.query(anyString())).thenReturn(response);

        boolean exists = adapter.existsInApplication(FUNCTIONALITY, APPLICATION);

        assertThat(exists).isTrue();
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(query.capture());
        assertThat(query.getValue()).contains("functionality:`" + FUNCTIONALITY + "`")
                .contains("application_id = application:`" + APPLICATION + "`");
    }

    @Test
    void existsInApplication_returnsFalseWhenScopedRecordIsAbsent() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.existsInApplication(FUNCTIONALITY, APPLICATION)).isFalse();
    }

    @Test
    void existsInApplication_wrapsQueryFailureAsTechnicalDomainException() {
        when(surreal.query(anyString())).thenThrow(new IllegalStateException("lookup failed"));

        assertThatThrownBy(() -> adapter.existsInApplication(FUNCTIONALITY, APPLICATION))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findStateNameById_mapsReturnedName() {
        Response response = rowsWithField("name", "Inactive");
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findStateNameById(STATE)).contains("Inactive");
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(query.capture());
        assertThat(query.getValue()).contains("functionality_state:`" + STATE + "`");
    }

    @Test
    void findStateNameById_returnsEmptyWhenStateIsAbsent() {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThat(adapter.findStateNameById(STATE)).isEmpty();
    }

    @Test
    void findStateNameById_wrapsQueryFailureAsTechnicalDomainException() {
        when(surreal.query(anyString())).thenThrow(new IllegalStateException("state lookup failed"));

        assertThatThrownBy(() -> adapter.findStateNameById(STATE)).isInstanceOf(BusinessException.class);
    }

    private static Value codeRows(String... codes) {
        Value result = mock(Value.class);
        when(result.isArray()).thenReturn(true);
        Array rows = mock(Array.class);
        when(rows.len()).thenReturn(codes.length);
        when(result.getArray()).thenReturn(rows);
        for (int index = 0; index < codes.length; index++) {
            Value rowValue = mock(Value.class);
            Object row = mock(Object.class);
            Value code = mock(Value.class);
            when(rowValue.isObject()).thenReturn(true);
            when(rowValue.getObject()).thenReturn(row);
            when(row.get("code")).thenReturn(code);
            when(code.isString()).thenReturn(true);
            when(code.getString()).thenReturn(codes[index]);
            when(rows.get(index)).thenReturn(rowValue);
        }
        return result;
    }

    private static Response rowsWithField(String fieldName, String fieldValue) {
        Response response = mock(Response.class);
        when(response.size()).thenReturn(1);
        Value result = mock(Value.class);
        when(result.isArray()).thenReturn(true);
        Array rows = mock(Array.class);
        when(result.getArray()).thenReturn(rows);
        when(rows.len()).thenReturn(1);
        Value rowValue = mock(Value.class);
        when(rowValue.isObject()).thenReturn(true);
        Object row = mock(Object.class);
        when(rowValue.getObject()).thenReturn(row);
        if ("name".equals(fieldName)) {
            Value field = mock(Value.class);
            when(field.isString()).thenReturn(true);
            when(field.getString()).thenReturn(fieldValue);
            when(row.get(fieldName)).thenReturn(field);
        }
        when(rows.get(0)).thenReturn(rowValue);
        when(response.take(0)).thenReturn(result);
        return response;
    }
}


