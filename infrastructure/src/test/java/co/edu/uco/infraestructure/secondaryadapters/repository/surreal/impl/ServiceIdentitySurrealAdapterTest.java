package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.surrealdb.Array;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceIdentitySurrealAdapter")
class ServiceIdentitySurrealAdapterTest {

    private static final String APP = "123e4567-e89b-12d3-a456-426614175802";
    private static final String ENV = "123e4567-e89b-12d3-a456-426614175804";

    @Mock private Surreal surreal;
    @Mock private LoggingPortFactory logs;
    @Mock private LoggingPort log;
    @Mock private CatalogPort catalog;
    private ServiceIdentitySurrealAdapter adapter;

    @BeforeEach
    void setUp() {
        when(logs.getLogger(ServiceIdentitySurrealAdapter.class)).thenReturn(log);
        lenient().when(catalog.getMessage("TCH_109")).thenReturn("identities failed");
        adapter = new ServiceIdentitySurrealAdapter(surreal, logs, catalog);
    }

    @Test
    @DisplayName("Busca por client_id escapado y mapea aplicación, ambiente y fecha")
    void findsByClientId() {
        var row = row("inventario-dev", text("application:`" + APP + "`"), text("environment:`" + ENV + "`"),
                text("develop"), dateTime());
        var response = responseWith(row);
        when(surreal.query(anyString())).thenReturn(response);

        var found = adapter.findByClientId("inv'dev");

        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("FROM environment_service_identity WHERE client_id = 'inv\\'dev' LIMIT 1;");
        assertThat(found).contains(new ServiceIdentityData("inventario-dev", APP, ENV, "develop", "2026-11-10T20:00:00Z"));
    }

    @Test
    @DisplayName("Lista las identidades de la aplicación ordenadas por fecha")
    void listsByApplication() {
        var row = row("inventario-dev", text(APP), text(ENV), text("develop"), text("2026-11-10T20:00:00Z"));
        var response = responseWith(row);
        when(surreal.query(anyString())).thenReturn(response);

        var identities = adapter.findByApplicationId(APP);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).contains("WHERE application_id = application:`" + APP + "` ORDER BY created_at;");
        assertThat(identities).containsExactly(
                new ServiceIdentityData("inventario-dev", APP, ENV, "develop", "2026-11-10T20:00:00Z"));
    }

    @Test
    @DisplayName("Crea la asociación con referencias a la aplicación y el ambiente")
    void registersIdentity() {
        var response = mock(Response.class);
        when(surreal.query(anyString())).thenReturn(response);

        adapter.register("inventario-dev", APP, ENV);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal).query(sql.capture());
        assertThat(sql.getValue()).isEqualTo("CREATE environment_service_identity CONTENT { client_id: 'inventario-dev'"
                + ", application_id: application:`" + APP + "`, environment_id: environment:`" + ENV + "` } RETURN NONE;");
        verify(response).take(0);
    }

    @Test
    @DisplayName("Envuelve como error técnico el fallo diferido del índice único")
    void wrapsDeferredRegisterFailure() {
        var response = mock(Response.class);
        doThrow(new IllegalStateException("unique index")).when(response).take(0);
        when(surreal.query(anyString())).thenReturn(response);

        assertThatThrownBy(() -> adapter.register("inventario-dev", APP, ENV))
                .isInstanceOf(BusinessException.class);
        verify(log).error(any(String.class), any(IllegalStateException.class));
    }

    @Test
    @DisplayName("Retira la identidad solo de la aplicación indicada y reporta si existía")
    void removesIdentity() {
        var deleted = responseWith(row("inventario-dev", text(APP), text(ENV), text(""), text("")));
        var empty = mock(Response.class);
        when(empty.size()).thenReturn(0);
        when(surreal.query(anyString())).thenReturn(deleted, empty);

        assertThat(adapter.remove("inventario-dev", APP)).isTrue();
        assertThat(adapter.remove("otra", APP)).isFalse();

        var sql = ArgumentCaptor.forClass(String.class);
        verify(surreal, org.mockito.Mockito.times(2)).query(sql.capture());
        assertThat(sql.getAllValues().get(0)).isEqualTo("DELETE environment_service_identity WHERE client_id = 'inventario-dev'"
                + " AND application_id = application:`" + APP + "` RETURN BEFORE;");
    }

    @Test
    @DisplayName("Convierte los fallos de consulta en errores técnicos del catálogo")
    void wrapsQueryFailures() {
        when(surreal.query(anyString())).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> adapter.findByClientId("x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.findByApplicationId(APP)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.remove("x", APP)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("No vuelve a envolver un error de negocio")
    void keepsBusinessExceptions() {
        var business = BusinessException.buildTechnicalException("ya envuelto", null,
                co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation.INFRASTRUCTURE);
        when(surreal.query(anyString())).thenThrow(business);

        assertThatThrownBy(() -> adapter.findByClientId("x")).isSameAs(business);
    }

    private static Value text(String value) {
        var mocked = mock(Value.class);
        lenient().when(mocked.isString()).thenReturn(true);
        lenient().when(mocked.getString()).thenReturn(value);
        return mocked;
    }

    private static Value dateTime() {
        var mocked = mock(Value.class);
        when(mocked.isDateTime()).thenReturn(true);
        when(mocked.getDateTime()).thenReturn(ZonedDateTime.of(2026, 11, 10, 20, 0, 0, 0, ZoneOffset.UTC));
        return mocked;
    }

    private static com.surrealdb.Object row(String clientId, Value application, Value environment, Value name, Value created) {
        var object = mock(com.surrealdb.Object.class);
        var fields = Map.of("client_id", text(clientId), "application_id", application, "environment_id", environment,
                "environment_name", name, "created_at", created);
        lenient().when(object.get(anyString())).thenAnswer(invocation -> fields.get(invocation.<String>getArgument(0)));
        return object;
    }

    private static Response responseWith(com.surrealdb.Object row) {
        var item = mock(Value.class);
        when(item.isObject()).thenReturn(true);
        when(item.getObject()).thenReturn(row);
        var array = mock(Array.class);
        when(array.len()).thenReturn(1);
        when(array.get(0)).thenReturn(item);
        var result = mock(Value.class);
        when(result.isArray()).thenReturn(true);
        when(result.getArray()).thenReturn(array);
        var response = mock(Response.class);
        when(response.size()).thenReturn(1);
        when(response.take(0)).thenReturn(result);
        return response;
    }
}
