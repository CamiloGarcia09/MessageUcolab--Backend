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
    private static final String PUBLIC_ORIGIN = "http://consumer.example";

    private final CorsWebFilter filter = new CorsConfig().corsWebFilter(
            List.of(ADMIN_ORIGIN, MESSAGES_ORIGIN), List.of(PUBLIC_ORIGIN));

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

    @Test
    @DisplayName("Mantiene CORS público sin credenciales y limitado a GET y OPTIONS")
    void keepsPublicCorsSeparatedFromAdminOrigins() {
        MockServerWebExchange exchange = preflight(
                "/messageucolab/v1/public/messages", PUBLIC_ORIGIN, "GET");

        filter.filter(exchange, completingChain()).block();

        assertAll(
                () -> assertThat(exchange.getResponse().getHeaders()
                        .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(PUBLIC_ORIGIN),
                () -> assertThat(exchange.getResponse().getHeaders()
                        .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull(),
                () -> assertThat(exchange.getResponse().getHeaders()
                        .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,OPTIONS"));
    }

    @Test
    @DisplayName("No amplía los orígenes públicos con los administrativos")
    void doesNotAllowAdminOriginOnPublicRoute() {
        MockServerWebExchange exchange = preflight(
                "/messageucolab/v1/public/messages", MESSAGES_ORIGIN, "GET");

        filter.filter(exchange, completingChain()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    @DisplayName("Rechaza métodos de escritura en la ruta pública")
    void rejectsWriteMethodsOnPublicRoute() {
        MockServerWebExchange exchange = preflight(
                "/messageucolab/v1/public/messages", PUBLIC_ORIGIN, "POST");

        filter.filter(exchange, completingChain()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
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
