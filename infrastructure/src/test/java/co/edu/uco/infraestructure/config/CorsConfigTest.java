package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("CorsConfig")
class CorsConfigTest {

    private static final String ADMIN_ORIGIN = "http://localhost:5174";
    private static final String MESSAGES_ORIGIN = "http://localhost:4200";

    private final CorsWebFilter filter = new CorsConfig().corsWebFilter(
            List.of(ADMIN_ORIGIN, MESSAGES_ORIGIN));

    @Test
    @DisplayName("Permite los orígenes administrativos configurados con credenciales y métodos de escritura")
    void allowsConfiguredAdminOriginsWithCredentialsAndWriteMethods() {
        for (String origin : List.of(ADMIN_ORIGIN, MESSAGES_ORIGIN)) {
            MockServerWebExchange exchange = preflight("/messageucolab/v1/application/functionality", origin, "POST");

            filter.filter(exchange, completingChain()).block();

            assertAll(
                    () -> assertThat(exchange.getResponse().getHeaders()
                            .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(origin),
                    () -> assertThat(exchange.getResponse().getHeaders()
                            .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true"),
                    () -> assertThat(exchange.getResponse().getHeaders()
                            .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                            .isEqualTo("GET,POST,PUT,PATCH,DELETE,OPTIONS"));
        }
    }

    @Test
    @DisplayName("No concede CORS a un origen administrativo ajeno")
    void rejectsUnconfiguredAdminOrigin() {
        MockServerWebExchange exchange = preflight(
                "/messageucolab/v1/application/functionality", "http://attacker.example", "POST");

        filter.filter(exchange, completingChain()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
    }

    private static MockServerWebExchange preflight(String path, String origin, String method) {
        return MockServerWebExchange.from(MockServerHttpRequest.options(URI.create("http://localhost" + path).toString())
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method));
    }

    private static WebFilterChain completingChain() {
        return (ServerWebExchange exchange) -> Mono.empty();
    }
}
