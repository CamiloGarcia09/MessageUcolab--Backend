package co.edu.uco.infraestructure.secondaryadapters.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.security.SessionIdentityPort;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SECURITY_SESSION_COOKIE;

@Component
public final class PdpSessionIdentityAdapter implements SessionIdentityPort {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient client;
    private final URI sessionUri;
    private final String identitySource;
    private final ObjectMapper mapper;
    private final CatalogPort catalog;

    @Autowired
    public PdpSessionIdentityAdapter(
            @Value("${components.security.session.pdp-url:http://localhost:18080}") String pdpUrl,
            @Value("${components.security.session.identity-source:security-baseline}") String identitySource,
            ObjectMapper mapper, CatalogPort catalog) {
        this(pdpUrl, identitySource, mapper, catalog, newHttpClient());
    }

    PdpSessionIdentityAdapter(String pdpUrl, String identitySource, ObjectMapper mapper, CatalogPort catalog,
                              HttpClient client) {
        this.sessionUri = URI.create(pdpUrl.replaceAll("/+$", "") + "/api/v1/session");
        this.identitySource = identitySource;
        this.mapper = mapper;
        this.catalog = catalog;
        this.client = client;
    }

    static HttpClient newHttpClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public SessionIdentity resolve(String session) {
        if (session == null || session.isBlank()) {
            throw unauthorized();
        }
        var request = HttpRequest.newBuilder(sessionUri).timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Cookie", SECURITY_SESSION_COOKIE + "=" + session).GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return switch (response.statusCode()) {
                case 200 -> parse(response.body());
                case 401 -> throw unauthorized();
                case 403 -> throw ForbiddenException.buildUserException(
                        catalog.getMessage(MessageCatalogCodeEnum.FUN_153.getCode()));
                default -> throw unavailable(null);
            };
        } catch (IOException exception) {
            throw unavailable(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        }
    }

    private SessionIdentity parse(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        if (root == null) {
            throw unavailable(null);
        }
        JsonNode data = root.path("data");
        if (!data.isObject() || !nonBlankText(data.path("subject")) || !nonBlankText(data.path("tenantId"))) {
            throw unavailable(null);
        }
        return new SessionIdentity(identitySource, data.path("tenantId").asText(), data.path("subject").asText(),
                data.path("email").asText(""), data.path("name").asText(""));
    }

    private static boolean nonBlankText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank();
    }

    private UnauthorizedException unauthorized() {
        return UnauthorizedException.buildUserException(catalog.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
    }

    private BusinessException unavailable(Exception cause) {
        return BusinessException.buildTechnicalException(catalog.getMessage(MessageCatalogCodeEnum.TCH_076.getCode()),
                cause, ExceptionLocation.INFRASTRUCTURE);
    }
}
