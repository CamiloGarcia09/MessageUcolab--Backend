package co.edu.uco.infraestructure.secondaryadapters.security;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.security.SecurityApplicationPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.CACHE_MAXIMUM_SIZE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SECURITY_SESSION_COOKIE;

/**
 * Consulta GET /api/v1/me/applications del PDP reenviando la cookie de la sesion BFF.
 * Las respuestas se conservan unos segundos por sesion (clave hash, nunca el valor)
 * para no duplicar la consulta en cada peticion del mismo usuario.
 */
@Component
public final class PdpSecurityApplicationAdapter implements SecurityApplicationPort {

    private static final String ME_APPLICATIONS_PATH = "/api/v1/me/applications";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CatalogPort catalogPort;
    private final Clock clock;
    private final URI applicationsUri;
    private final Map<String, CachedApplications> cache = new ConcurrentHashMap<>();

    @Autowired
    public PdpSecurityApplicationAdapter(@Value("${components.security.session.pdp-url:http://localhost:18080}")
                                         final String pdpUrl,
                                         final ObjectMapper objectMapper,
                                         final CatalogPort catalogPort) {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
                pdpUrl, objectMapper, catalogPort, Clock.systemUTC());
    }

    PdpSecurityApplicationAdapter(final HttpClient httpClient, final String pdpUrl, final ObjectMapper objectMapper,
                                  final CatalogPort catalogPort, final Clock clock) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.catalogPort = catalogPort;
        this.clock = clock;
        this.applicationsUri = URI.create(pdpUrl.replaceAll("/+$", "") + ME_APPLICATIONS_PATH);
    }

    @Override
    public List<CatalogItemDTO> findCurrentUserApplications(final String session) {
        var key = hash(session);
        var now = clock.instant();
        var cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.applications();
        }
        var applications = request(session);
        if (cache.size() >= CACHE_MAXIMUM_SIZE) {
            cache.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
        }
        cache.put(key, new CachedApplications(applications, now.plus(CACHE_TTL)));
        return applications;
    }

    private List<CatalogItemDTO> request(final String session) {
        var request = HttpRequest.newBuilder(applicationsUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Cookie", SECURITY_SESSION_COOKIE + "=" + session)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw unavailable(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        }
        return switch (response.statusCode()) {
            case 200 -> parse(response.body());
            case 401 -> throw UnauthorizedException.buildUserException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
            case 403 -> throw ForbiddenException.buildUserException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode()));
            default -> throw unavailable(new IllegalStateException(
                    "PDP respondio " + response.statusCode() + " al consultar las aplicaciones del usuario"));
        };
    }

    private List<CatalogItemDTO> parse(final String body) {
        try {
            JsonNode applications = objectMapper.readTree(body).path("data").path("applications");
            var result = new ArrayList<CatalogItemDTO>();
            for (JsonNode application : applications) {
                result.add(CatalogItemDTO.create(application.path("id").asText(""),
                        application.path("name").asText("")));
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw unavailable(exception);
        }
    }

    private BusinessException unavailable(final Exception exception) {
        return BusinessException.buildTechnicalException(
                "No fue posible consultar las aplicaciones del usuario en el componente de seguridad",
                exception, ExceptionLocation.INFRASTRUCTURE);
    }

    private static String hash(final String session) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(session.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record CachedApplications(List<CatalogItemDTO> applications, Instant expiresAt) {
    }
}
