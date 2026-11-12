package co.edu.uco.infraestructure.primaryadapters.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.io.IOException;
import java.util.Base64;

/**
 * Lee el client_id del token de una identidad de servicio. No valida la firma: la peticion solo llega al
 * controlador despues de que el PEP valido ese mismo token y autorizo la ruta.
 */
@Component
public final class ServiceIdentityClientIdReader {

    private static final String BEARER = "Bearer ";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Devuelve el client_id ({@code azp}, o {@code client_id}) o null si el token no lo trae. */
    public String read(final ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String[] parts = header.substring(BEARER.length()).trim().split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            JsonNode claims = MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
            String authorizedParty = text(claims, "azp");
            return authorizedParty != null ? authorizedParty : text(claims, "client_id");
        } catch (IllegalArgumentException | IOException exception) {
            return null;
        }
    }

    private static String text(final JsonNode claims, final String name) {
        JsonNode value = claims == null ? null : claims.get(name);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
