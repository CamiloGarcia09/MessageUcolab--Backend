package co.edu.uco.infraestructure.secondaryadapters.parameter.catalogo;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.parameter.ParameterValuePort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.config.CatalogoParametrosProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Consulta los parámetros de MessageUcoLab en el componente de Parámetros (Catálogo de Parámetros).
 *
 * <p>Contrato del Catálogo: {@code GET <base-url>/tokens/<tokenId>/parametros} con
 * {@code Authorization: Bearer <secreto>}. El token decide la aplicación y el ambiente; la respuesta trae los
 * parámetros autorizados y el valor de cada uno está en {@code metadatos[].metadato.valor}.</p>
 *
 * <p>Los parámetros se guardan en memoria durante {@code catalogo.parametros.cache-ttl} para no consultar el
 * Catálogo en cada petición. Si el Catálogo falla, se conservan los últimos valores conocidos (o ninguno) hasta
 * el siguiente intento. Nunca lanza excepciones ni registra el secreto.</p>
 */
@Component
public final class CatalogoParametrosHttpAdapter implements ParameterValuePort {

    private static final String PARAMETERS_PATH = "/tokens/%s/parametros";

    private final HttpClient httpClient;
    private final CatalogoParametrosProperties properties;
    private final ObjectMapper objectMapper;
    private final CatalogPort catalogPort;
    private final LoggingPort log;
    private final Clock clock;

    private volatile Snapshot snapshot = Snapshot.EMPTY;

    @Autowired
    public CatalogoParametrosHttpAdapter(final CatalogoParametrosProperties properties, final ObjectMapper objectMapper,
                                         final CatalogPort catalogPort, final LoggingPortFactory loggerFactory) {
        this(HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build(), properties, objectMapper,
                catalogPort, loggerFactory, Clock.systemUTC());
    }

    CatalogoParametrosHttpAdapter(final HttpClient httpClient, final CatalogoParametrosProperties properties,
                                  final ObjectMapper objectMapper, final CatalogPort catalogPort,
                                  final LoggingPortFactory loggerFactory, final Clock clock) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(CatalogoParametrosHttpAdapter.class);
        this.clock = clock;
    }

    @Override
    public Optional<String> findValue(final String parameterName) {
        if (parameterName == null || parameterName.isBlank() || !properties.isConfigured()) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentValues().get(parameterName.trim()));
    }

    private Map<String, String> currentValues() {
        final var now = clock.instant();
        var current = snapshot;
        if (current.isValidAt(now)) {
            return current.values();
        }
        synchronized (this) {
            current = snapshot;
            if (!current.isValidAt(now)) {
                snapshot = new Snapshot(refresh(current.values()), now.plus(properties.getCacheTtl()));
            }
            return snapshot.values();
        }
    }

    // Si la consulta falla se conservan los últimos valores conocidos; el siguiente intento llega al vencer el TTL.
    private Map<String, String> refresh(final Map<String, String> lastKnown) {
        try {
            final var response = httpClient.send(request(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                logFailure("HTTP " + response.statusCode(), null);
                return lastKnown;
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logFailure(exception.getClass().getSimpleName(), exception);
            return lastKnown;
        } catch (IOException | RuntimeException exception) {
            logFailure(exception.getClass().getSimpleName(), exception);
            return lastKnown;
        }
    }

    private HttpRequest request() {
        final var tokenId = URLEncoder.encode(properties.getTokenId().trim(), StandardCharsets.UTF_8);
        final var uri = URI.create(properties.getBaseUrl().trim().replaceAll("/+$", "")
                + PARAMETERS_PATH.formatted(tokenId));
        return HttpRequest.newBuilder(uri)
                .timeout(properties.getTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.getSecreto().trim())
                .GET()
                .build();
    }

    // Toma los parámetros activos y el primer valor no vacío de sus metadatos.
    private Map<String, String> parse(final String body) throws IOException {
        final Map<String, String> values = new HashMap<>();
        for (JsonNode item : objectMapper.readTree(body).path("parametros")) {
            final var parameter = item.path("parametro");
            final var name = parameter.path("nombre").asText("").trim();
            if (name.isEmpty() || !parameter.path("activo").asBoolean(true)) {
                continue;
            }
            for (JsonNode metadata : item.path("metadatos")) {
                final var value = metadata.path("metadato").path("valor").asText("").trim();
                if (!value.isEmpty()) {
                    values.putIfAbsent(name, value);
                    break;
                }
            }
        }
        return Map.copyOf(values);
    }

    private void logFailure(final String reason, final Throwable error) {
        final var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_110.getCode());
        if (error == null) {
            log.warn(message, reason);
        } else {
            log.warn(message, reason, error);
        }
    }

    private record Snapshot(Map<String, String> values, Instant expiresAt) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Instant.MIN);

        boolean isValidAt(final Instant now) {
            return expiresAt.isAfter(now);
        }
    }
}
