package co.edu.uco.infraestructure.secondaryadapters.parameter.catalogo;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.infraestructure.config.CatalogoParametrosProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatalogoParametrosHttpAdapter")
class CatalogoParametrosHttpAdapterTest {

    private static final String RESPONSE = """
            {
              "ambiente": { "id": "env-1", "nombre": "Develop" },
              "parametros": [
                { "parametro": { "nombre": "TRADUCCION_HABILITADA", "activo": true },
                  "metadatos": [ { "metadato": { "valor": "" } }, { "metadato": { "valor": "true" } } ] },
                { "parametro": { "nombre": "OTRO_INACTIVO", "activo": false },
                  "metadatos": [ { "metadato": { "valor": "x" } } ] },
                { "parametro": { "nombre": "SIN_VALOR", "activo": true }, "metadatos": [] }
              ]
            }
            """;

    @Mock HttpClient httpClient;
    @Mock CatalogPort catalogPort;
    @Mock LoggingPortFactory loggerFactory;
    @Mock LoggingPort log;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-14T10:00:00Z"));
    private CatalogoParametrosProperties properties;
    private CatalogoParametrosHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new CatalogoParametrosProperties();
        properties.setBaseUrl("http://host.docker.internal:8081/catalogo-parametros/api/v1/");
        properties.setTokenId("tok-123");
        properties.setSecreto("secreto-del-token");
        properties.setCacheTtl(Duration.ofSeconds(60));
        when(loggerFactory.getLogger(CatalogoParametrosHttpAdapter.class)).thenReturn(log);
        when(catalogPort.getMessage("TCH_110")).thenReturn("parametros-error {}");
        adapter = new CatalogoParametrosHttpAdapter(httpClient, properties, new ObjectMapper(), catalogPort,
                loggerFactory, clock);
    }

    @SuppressWarnings("unchecked")
    private void respond(final int status, final String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test
    @DisplayName("Lee el valor del parámetro activo con el contrato del Catálogo (URL del token y Bearer)")
    void readsActiveParameterValue() throws Exception {
        respond(200, RESPONSE);

        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).contains("true");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any());
        assertThat(request.getValue().uri().toString())
                .isEqualTo("http://host.docker.internal:8081/catalogo-parametros/api/v1/tokens/tok-123/parametros");
        assertThat(request.getValue().headers().firstValue("Authorization")).contains("Bearer secreto-del-token");
        assertThat(request.getValue().headers().firstValue("Accept")).contains("application/json");
    }

    @Test
    @DisplayName("Un parámetro inactivo, sin valor o inexistente se devuelve vacío")
    void inactiveMissingOrEmptyIsEmpty() throws Exception {
        respond(200, RESPONSE);

        assertThat(adapter.findValue("OTRO_INACTIVO")).isEmpty();
        assertThat(adapter.findValue("SIN_VALOR")).isEmpty();
        assertThat(adapter.findValue("NO_EXISTE")).isEmpty();
    }

    @Test
    @DisplayName("Sin URL, token o secreto no consulta el Catálogo")
    void notConfiguredDoesNotCall() throws Exception {
        properties.setSecreto(" ");

        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).isEmpty();
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    @DisplayName("Guarda los parámetros durante el TTL y los vuelve a consultar al vencer")
    void cachesDuringTtl() throws Exception {
        respond(200, RESPONSE);

        adapter.findValue("TRADUCCION_HABILITADA");
        adapter.findValue("TRADUCCION_HABILITADA");
        verify(httpClient, times(1)).send(any(), any());

        clock.advance(Duration.ofSeconds(61));
        adapter.findValue("TRADUCCION_HABILITADA");
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @DisplayName("Si el Catálogo falla conserva el último valor conocido y registra el error sin el secreto")
    void failureKeepsLastKnownValue() throws Exception {
        respond(200, RESPONSE);
        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).contains("true");

        clock.advance(Duration.ofSeconds(61));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).contains("true");
        verify(log).warn(eq("parametros-error {}"), eq("IOException"), any(IOException.class));
    }

    @Test
    @DisplayName("Una respuesta distinta de 200 sin valores previos devuelve vacío y no lanza excepción")
    void unauthorizedWithoutPreviousValuesIsEmpty() throws Exception {
        respond(401, "{\"error\":\"TOKEN_INVALID\"}");

        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).isEmpty();
        verify(log).warn(eq("parametros-error {}"), eq("HTTP 401"));
    }

    @Test
    @DisplayName("Un cuerpo ilegible no lanza excepción")
    void unreadableBodyIsEmpty() throws Exception {
        respond(200, "no-es-json{");

        assertThat(adapter.findValue("TRADUCCION_HABILITADA")).isEmpty();
        verify(log).warn(eq("parametros-error {}"), anyString(), any(Throwable.class));
    }

    @Test
    @DisplayName("Un nombre vacío no consulta")
    void blankNameDoesNotCall() throws Exception {
        assertThat(adapter.findValue(" ")).isEmpty();
        assertThat(adapter.findValue(null)).isEmpty();
        verify(httpClient, never()).send(any(), any());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(final Instant now) {
            this.now = now;
        }

        void advance(final Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
