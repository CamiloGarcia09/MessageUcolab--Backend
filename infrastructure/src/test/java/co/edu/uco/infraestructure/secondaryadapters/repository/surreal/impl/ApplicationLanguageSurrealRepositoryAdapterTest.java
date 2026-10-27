package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationLanguageSurrealRepositoryAdapterTest {
    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort log;
    @Mock private CatalogPort catalog;

    private ApplicationLanguageSurrealRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(ApplicationLanguageSurrealRepositoryAdapter.class)).thenReturn(log);
        when(catalog.getMessage("TCH_058")).thenReturn("query error: %s");
        adapter = new ApplicationLanguageSurrealRepositoryAdapter(surreal, loggerFactory, catalog);
    }

    @Test
    void findBaseLanguageCode_mapsReferencedCodeAndQuotesSafeRecordId() {
        var row = org.mockito.Mockito.mock(Object.class);
        var code = org.mockito.Mockito.mock(Value.class);
        when(code.isString()).thenReturn(true);
        when(code.getString()).thenReturn("EN");
        when(row.get("code")).thenReturn(code);
        doReturn(response(row)).when(surreal).query(anyString());

        var result = adapter.findBaseLanguageCode("123e4567-e89b-12d3-a456-426614175030");

        assertThat(result).contains("EN");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).isEqualTo("SELECT language_id.code AS code FROM application:`123e4567-e89b-12d3-a456-426614175030` LIMIT 1;");
    }

    @Test
    void findBaseLanguageCode_mapsMissingReferenceToEmptyCodeForValidation() {
        var row = org.mockito.Mockito.mock(Object.class);
        when(row.get("code")).thenReturn(null);
        doReturn(response(row)).when(surreal).query(anyString());

        assertThat(adapter.findBaseLanguageCode("app-id")).contains("");
    }

    @Test
    void findBaseLanguageCode_mapsNoneAndNullCodesToEmptyString() {
        for (boolean none : new boolean[]{true, false}) {
            var row = org.mockito.Mockito.mock(Object.class);
            var code = org.mockito.Mockito.mock(Value.class);
            org.mockito.Mockito.lenient().when(code.isNone()).thenReturn(none);
            org.mockito.Mockito.lenient().when(code.isNull()).thenReturn(!none);
            when(row.get("code")).thenReturn(code);
            doReturn(response(row)).when(surreal).query(anyString());

            assertThat(adapter.findBaseLanguageCode("app-id")).contains("");
        }
    }

    @Test
    void findBaseLanguageCode_preservesCrossWordsDatabaseFailure() {
        var failure = CrossWordsException.buildInfrastructure("database failure",
                new IllegalStateException("database unavailable"));
        doThrow(failure).when(surreal).query(anyString());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findBaseLanguageCode("app-id"))
                .isSameAs(failure);
    }

    @Test
    void findBaseLanguageCode_wrapsUnexpectedClientFailure() {
        when(catalog.getMessage("TCH_058")).thenReturn("query error: %s");
        doThrow(new IllegalStateException("client failure")).when(surreal).query(anyString());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findBaseLanguageCode("app-id"))
                .isInstanceOf(CrossWordsException.class);
    }

    private static Response response(Object row) {
        Value item = org.mockito.Mockito.mock(Value.class);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(row);
        Array rows = org.mockito.Mockito.mock(Array.class);
        when(rows.len()).thenReturn(1);
        when(rows.get(0)).thenReturn(item);
        Value statement = org.mockito.Mockito.mock(Value.class);
        when(statement.isArray()).thenReturn(true);
        when(statement.getArray()).thenReturn(rows);
        Response response = org.mockito.Mockito.mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(statement);
        return response;
    }
}
