package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Array;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationSurrealAdapterTest {
    @Mock Surreal surreal;
    @Mock LoggingPortFactory logs;
    @Mock LoggingPort log;
    @Mock CatalogPort catalog;

    @BeforeEach
    void setUp() {
        when(logs.getLogger(DeleteApplicationSurrealAdapter.class)).thenReturn(log);
        lenient().when(catalog.getMessage("FUN_231")).thenReturn("ownership changed");
        lenient().when(catalog.getMessage("TCH_107")).thenReturn("database delete failed");
    }

    @Test
    void deleteAtomically_buildsSingleOwnershipGuardedTransactionWithoutSharedOrAuditDeletes() {
        var userId = UUID.randomUUID();
        var appId = UUID.randomUUID();
        var response = mock(Response.class);
        when(response.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(response);

        new DeleteApplicationSurrealAdapter(surreal, logs, catalog).deleteAtomically(userId, appId);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        var transaction = sql.getValue();
        assertThat(transaction).startsWith("BEGIN TRANSACTION;").contains("organization_id.owner_user_id = app_user:`" + userId + "`")
                .contains("IF array::len($owned) != 1")
                .contains("DELETE message_environment_readmodel").contains("DELETE message_data_collection")
                .contains("DELETE environment_document").contains("DELETE application_document")
                .contains("DELETE message_environment").contains("DELETE message WHERE")
                .contains("DELETE functionality WHERE").contains("DELETE message_import_receipt WHERE")
                .contains("DELETE environment WHERE").contains("DELETE $app RETURN NONE;")
                .endsWith("COMMIT TRANSACTION;")
                .doesNotContain("DELETE organization", "DELETE domain_event", "DELETE message_type", "DELETE message_category");
        assertThat(transaction.indexOf("IF array::len($owned)")).isLessThan(transaction.indexOf("DELETE message_environment_readmodel"));
    }

    @Test
    void deleteAtomically_consumesEveryDeferredResponseAndWrapsDeferredFailure() {
        var response = mock(Response.class);
        when(catalog.getMessage("TCH_107")).thenReturn("database delete failed");
        when(response.size()).thenReturn(2);
        when(response.take(0)).thenReturn(null);
        var deferred = new IllegalStateException("deferred failure");
        doThrow(deferred).when(response).take(1);
        when(surreal.query(anyString())).thenReturn(response);

        assertThatThrownBy(() -> new DeleteApplicationSurrealAdapter(surreal, logs, catalog)
                .deleteAtomically(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getRootException()).isSameAs(deferred));
        verify(response).take(0);
        verify(response).take(1);
    }

    @Test
    void ownsApplication_returnsTrueOnlyWhenOwnershipQueryReturnsARecord() {
        var user = UUID.randomUUID();
        var app = UUID.randomUUID();
        var response = mock(Response.class);
        var statement = mock(Value.class);
        var rows = mock(Array.class);
        var row = mock(Value.class);
        when(surreal.query(anyString())).thenReturn(response);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(rows);
        when(rows.len()).thenReturn(1);
        when(rows.get(0)).thenReturn(row);
        when(row.isObject()).thenReturn(true);

        assertThat(new DeleteApplicationSurrealAdapter(surreal, logs, catalog).ownsApplication(user, app)).isTrue();
        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("SELECT id FROM application:`" + app + "`")
                .contains("organization_id.owner_user_id = app_user:`" + user + "`");
    }

    @Test
    void ownsApplication_returnsFalseWhenApplicationDoesNotExist() {
        var response = mock(Response.class);
        when(surreal.query(anyString())).thenReturn(response);
        when(response.size()).thenReturn(0);

        assertThat(new DeleteApplicationSurrealAdapter(surreal, logs, catalog)
                .ownsApplication(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    void deleteAtomically_consumesAllSuccessfulStatementResults() {
        var response = mock(Response.class);
        var first = mock(Value.class);
        var second = mock(Value.class);
        when(surreal.query(anyString())).thenReturn(response);
        when(response.size()).thenReturn(2);
        when(response.take(0)).thenReturn(first);
        when(response.take(1)).thenReturn(second);
        when(first.isArray()).thenReturn(false);
        when(second.isArray()).thenReturn(false);

        assertThat(new DeleteApplicationSurrealAdapter(surreal, logs, catalog)
                .deleteAtomically(UUID.randomUUID(), UUID.randomUUID())).isEmpty();

        verify(response).take(0);
        verify(response).take(1);
    }
}
