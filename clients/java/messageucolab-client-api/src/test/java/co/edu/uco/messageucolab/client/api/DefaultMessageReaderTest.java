package co.edu.uco.messageucolab.client.api;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import co.edu.uco.messageucolab.client.api.port.MessageFallbackPort;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DefaultMessageReader")
class DefaultMessageReaderTest {

    private MessageQueryPort query;
    private MessageFallbackPort fallback;
    private DefaultMessageReader reader;
    private final MessageDetails message = new MessageDetails("welcome", "en", "es", "Hello {name}", "Hi {name}",
            "INFO", "general", "home", Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z"));

    @BeforeEach
    void setUp() {
        query = mock(MessageQueryPort.class);
        fallback = mock(MessageFallbackPort.class);
        reader = new DefaultMessageReader(query, fallback);
    }

    @Nested
    @DisplayName("consulta detalles")
    class Details {
        @Test
        @DisplayName("devuelve el mensaje consultado por código")
        void returnsDetailsByCode() {
            when(query.findByCode("welcome")).thenReturn(Mono.just(message));

            assertEquals(message, reader.getDetails("welcome").block());
            verify(query).findByCode("welcome");
        }

        @Test
        @DisplayName("consulta el mensaje con idioma explícito")
        void returnsDetailsForLanguage() {
            when(query.findByCode("welcome", "es")).thenReturn(Mono.just(message));

            assertEquals(message, reader.getDetails("welcome", "es").block());
            verify(query).findByCode("welcome", "es");
        }

        @Test
        @DisplayName("conserva una consulta vacía cuando no hay coincidencia")
        void returnsEmptyWhenDetailsAreMissing() {
            when(query.findByCode("missing")).thenReturn(Mono.empty());

            assertNull(reader.getDetails("missing").block());
            verify(fallback, never()).findByCode("missing");
        }
    }

    @Nested
    @DisplayName("fallback")
    class Fallback {
        @Test
        @DisplayName("usa fallback para respuesta 404")
        void usesFallbackForNotFound() {
            MessageDetails cached = new MessageDetails("welcome", "en", "en", "Fallback", "cached", "INFO", "general", "home", null, null);
            when(query.findByCode("welcome")).thenReturn(Mono.error(new MessageClientResponseException(404, null)));
            when(fallback.findByCode("welcome")).thenReturn(Mono.just(cached));

            assertEquals(cached, reader.getDetails("welcome").block());
            verify(fallback).findByCode("welcome");
        }

        @Test
        @DisplayName("usa fallback para consulta que especifica idioma")
        void usesFallbackForLanguageSpecificNotFound() {
            MessageDetails cached = new MessageDetails("welcome", "en", "es", "Cached title", "Cached content",
                    "INFO", "general", "home", null, null);
            when(query.findByCode("welcome", "es")).thenReturn(Mono.error(new MessageClientResponseException(404, null)));
            when(fallback.findByCode("welcome")).thenReturn(Mono.just(cached));

            assertEquals(cached, reader.getDetails("welcome", "es").block());
            verify(query).findByCode("welcome", "es");
            verify(fallback).findByCode("welcome");
        }

        @Test
        @DisplayName("no usa fallback para error 4xx distinto de 404")
        void rejectsFallbackForForbiddenResponse() {
            MessageClientResponseException error = new MessageClientResponseException(403, null);
            when(query.findByCode("welcome")).thenReturn(Mono.error(error));

            assertSame(error, assertThrows(MessageClientResponseException.class, () -> reader.getDetails("welcome").block()));
        }

        @Test
        @DisplayName("usa fallback permitido ante error de transporte")
        void usesFallbackForTransportError() {
            when(query.findByCode("welcome")).thenReturn(Mono.error(new MessageClientTransportException(new IllegalStateException("offline"))));
            when(fallback.findByCode("welcome")).thenReturn(Mono.just(message));

            assertEquals(message, reader.getDetails("welcome").block());
        }

        @Test
        @DisplayName("con fallback vacío conserva el error original")
        void propagatesOriginalErrorWhenFallbackIsEmpty() {
            MessageClientResponseException original = new MessageClientResponseException(503, null);
            when(query.findByCode("welcome")).thenReturn(Mono.error(original));
            when(fallback.findByCode("welcome")).thenReturn(Mono.empty());

            assertSame(original, assertThrows(MessageClientResponseException.class, () -> reader.getDetails("welcome").block()));
        }

        @Test
        @DisplayName("no intenta fallback ante errores ajenos a la API")
        void doesNotFallbackForUnexpectedErrors() {
            IllegalArgumentException original = new IllegalArgumentException("unexpected");
            when(query.findByCode("welcome")).thenReturn(Mono.error(original));

            assertSame(original, assertThrows(IllegalArgumentException.class, () -> reader.getDetails("welcome").block()));
            verify(fallback, never()).findByCode("welcome");
        }
    }

    @Test
    @DisplayName("consulta detalles con idioma y lista completa")
    void passesLanguageAndReturnsCollection() {
        MessageCollection collection = new MessageCollection("es", 1, java.util.List.of(message));
        when(query.findByCode("welcome", "es")).thenReturn(Mono.just(message));
        when(query.findAll()).thenReturn(Mono.just(collection));

        assertEquals(message, reader.getDetails("welcome", "es").block());
        assertEquals(collection, reader.getAll().block());
        verify(query).findByCode("welcome", "es");
        verify(query).findAll();
    }

    @Test
    @DisplayName("permite consultar una colección sin mensajes")
    void returnsEmptyCollection() {
        MessageCollection empty = new MessageCollection("es", 0, java.util.List.of());
        when(query.findAll()).thenReturn(Mono.just(empty));

        assertEquals(empty, reader.getAll().block());
        verify(query).findAll();
    }

    @Test
    @DisplayName("expone solo los tres métodos públicos del contrato")
    void exposesOnlySupportedPublicMethods() {
        Set<String> actualMethods = Arrays.stream(MessageReader.class.getDeclaredMethods())
                .map(DefaultMessageReaderTest::signature)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(
                "getDetails(java.lang.String):reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageDetails>",
                "getDetails(java.lang.String,java.lang.String):reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageDetails>",
                "getAll():reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageCollection>"),
                actualMethods);
    }

    private static String signature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(java.util.stream.Collectors.joining(","));
        return method.getName() + "(" + parameters + "):" + method.getGenericReturnType().getTypeName();
    }
}
