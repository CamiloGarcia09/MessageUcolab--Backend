package co.edu.uco.infraestructure.primaryadapters.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceIdentityClientIdReader")
class ServiceIdentityClientIdReaderTest {

    private static final String HEADER = encode("{\"alg\":\"RS256\"}");

    private final ServiceIdentityClientIdReader reader = new ServiceIdentityClientIdReader();

    @Test
    @DisplayName("Lee el client_id del claim azp")
    void readsAuthorizedParty() {
        assertThat(reader.read(exchange("Bearer " + token("{\"azp\":\"inventario-dev\",\"client_id\":\"otro\"}"))))
                .isEqualTo("inventario-dev");
    }

    @Test
    @DisplayName("Usa el claim client_id cuando el token no trae azp")
    void fallsBackToClientIdClaim() {
        assertThat(reader.read(exchange("Bearer " + token("{\"client_id\":\"inventario-dev\"}"))))
                .isEqualTo("inventario-dev");
    }

    @Test
    @DisplayName("Ignora un azp vacío o que no es texto")
    void ignoresBlankOrNonTextAuthorizedParty() {
        assertThat(reader.read(exchange("Bearer " + token("{\"azp\":\"  \",\"client_id\":\"inventario-dev\"}"))))
                .isEqualTo("inventario-dev");
        assertThat(reader.read(exchange("Bearer " + token("{\"azp\":42}")))).isNull();
    }

    @Test
    @DisplayName("Acepta el esquema Bearer sin distinguir mayúsculas")
    void acceptsCaseInsensitiveScheme() {
        assertThat(reader.read(exchange("bearer " + token("{\"azp\":\"inventario-dev\"}")))).isEqualTo("inventario-dev");
    }

    @Test
    @DisplayName("Devuelve null sin encabezado o con otro esquema")
    void returnsNullWithoutBearerHeader() {
        assertThat(reader.read(exchange(null))).isNull();
        assertThat(reader.read(exchange("Basic dXNlcjpwYXNz"))).isNull();
    }

    @Test
    @DisplayName("Devuelve null si el token no tiene tres partes o no se puede decodificar")
    void returnsNullForMalformedToken() {
        assertThat(reader.read(exchange("Bearer solo.dos"))).isNull();
        assertThat(reader.read(exchange("Bearer " + HEADER + ".%%%.firma"))).isNull();
        assertThat(reader.read(exchange("Bearer " + HEADER + "." + encode("no es json") + ".firma"))).isNull();
        assertThat(reader.read(exchange("Bearer " + token("{}")))).isNull();
    }

    private static String token(String claims) {
        return HEADER + "." + encode(claims) + ".firma";
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static MockServerWebExchange exchange(String authorization) {
        var request = MockServerHttpRequest.get("/messageucolab/v1/messages/consumer");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(request.build());
    }
}
