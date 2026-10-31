package co.edu.uco.messageucolab.client.http;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MessageUcoLabHttpAdapter")
class MessageUcoLabHttpAdapterTest {

    private static final String ITEM_JSON = """
            {"code":"WELCOME","sourceLanguage":"en","targetLanguage":"es","title":"Hola","content":"Contenido",
             "type":"INFO","category":"general","functionality":"home","created_at":"2025-01-01T00:00:00Z",
             "updated_at":"2025-01-02T00:00:00Z"}
            """;

    @Nested
    @DisplayName("consulta individual")
    class SingleMessage {
        @Test
        @DisplayName("mapea campos y agrega cabeceras e idioma opcional")
        void mapsResponseAndSendsContextAndLanguage() {
            AtomicReference<org.springframework.web.reactive.function.client.ClientRequest> request = new AtomicReference<>();
            MessageUcoLabHttpAdapter adapter = adapter(req -> {
                request.set(req);
                return response(HttpStatus.OK, ITEM_JSON);
            }, Duration.ofSeconds(2));

            MessageDetails result = adapter.findByCode("WELCOME", "es").block();

            assertAll(() -> assertEquals("en", result.sourceLanguage()),
                    () -> assertEquals("es", result.targetLanguage()),
                    () -> assertEquals(Instant.parse("2025-01-01T00:00:00Z"), result.createdAt()),
                    () -> assertEquals(Instant.parse("2025-01-02T00:00:00Z"), result.updatedAt()),
                    () -> assertEquals("app-1", request.get().headers().getFirst("X-Application-Id")),
                    () -> assertEquals("env-1", request.get().headers().getFirst("X-Environment-Id")),
                    () -> assertEquals("language=es", request.get().url().getRawQuery()));
        }

        @Test
        @DisplayName("omite idioma cuando no se especifica")
        void omitsLanguageWhenAbsent() {
            AtomicReference<org.springframework.web.reactive.function.client.ClientRequest> request = new AtomicReference<>();
            MessageUcoLabHttpAdapter adapter = adapter(req -> {
                request.set(req);
                return response(HttpStatus.OK, ITEM_JSON);
            }, Duration.ofSeconds(2));

            adapter.findByCode("WELCOME").block();

            assertNull(request.get().url().getRawQuery());
        }

        @Test
        @DisplayName("omite idioma en blanco")
        void omitsBlankLanguage() {
            AtomicReference<org.springframework.web.reactive.function.client.ClientRequest> request = new AtomicReference<>();
            MessageUcoLabHttpAdapter adapter = adapter(req -> {
                request.set(req);
                return response(HttpStatus.OK, ITEM_JSON);
            }, Duration.ofSeconds(2));

            adapter.findByCode("WELCOME", "  ").block();

            assertNull(request.get().url().getRawQuery());
        }

        @Test
        @DisplayName("convierte respuesta HTTP de error en excepción tipada")
        void mapsHttpFailure() {
            MessageUcoLabHttpAdapter adapter = adapter(req -> response(HttpStatus.NOT_FOUND, "{}"), Duration.ofSeconds(2));

            MessageClientResponseException error = assertThrows(MessageClientResponseException.class,
                    () -> adapter.findByCode("MISSING").block());

            assertEquals(404, error.getStatusCode());
        }

        @Test
        @DisplayName("mapea timeout como error de transporte")
        void mapsTimeoutAsTransportFailure() {
            MessageUcoLabHttpAdapter adapter = adapter(req -> Mono.never(), Duration.ofMillis(1));

            assertThrows(MessageClientTransportException.class, () -> adapter.findByCode("WELCOME").block());
        }

        @Test
        @DisplayName("conserva errores de transporte ya tipados")
        void preservesExistingTransportException() {
            MessageClientTransportException original = new MessageClientTransportException(new IllegalStateException("offline"));
            MessageUcoLabHttpAdapter adapter = adapter(req -> Mono.error(original), Duration.ofSeconds(2));

            MessageClientTransportException error = assertThrows(MessageClientTransportException.class,
                    () -> adapter.findByCode("WELCOME").block());

            assertSame(original, error);
        }
    }

    @Test
    @DisplayName("mapea listado con idiomas y marcas de tiempo de cada mensaje")
    void mapsMessageList() {
        String json = "{\"language\":\"es\",\"total\":1,\"messages\":[" + ITEM_JSON + "]}";
        MessageUcoLabHttpAdapter adapter = adapter(req -> response(HttpStatus.OK, json), Duration.ofSeconds(2));

        MessageCollection result = adapter.findAll().block();
        MessageDetails item = result.messages().getFirst();

        assertAll(() -> assertEquals("es", result.language()), () -> assertEquals(1, result.total()),
                () -> assertEquals("en", item.sourceLanguage()), () -> assertEquals("es", item.targetLanguage()),
                () -> assertEquals(Instant.parse("2025-01-01T00:00:00Z"), item.createdAt()),
                () -> assertEquals(Instant.parse("2025-01-02T00:00:00Z"), item.updatedAt()));
    }

    private static MessageUcoLabHttpAdapter adapter(ExchangeFunction exchange, Duration timeout) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        return new MessageUcoLabHttpAdapter(builder, new MessageUcoLabProperties("https://messages.example", "app-1", "env-1", timeout));
    }

    private static Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status).header("Content-Type", "application/json").body(body).build());
    }
}
