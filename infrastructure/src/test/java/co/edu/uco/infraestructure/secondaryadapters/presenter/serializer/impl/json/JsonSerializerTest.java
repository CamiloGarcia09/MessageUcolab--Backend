package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.Response;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonSerializerTest {

    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    private final JsonSerializer serializer = new JsonSerializer();

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getSupportedContentType_returnsApplicationJson() {
        assertThat(serializer.getSupportedContentType()).isEqualTo("application/json");
    }

    @Test
    void isDefault_returnsTrue() {
        assertThat(serializer.isDefault()).isTrue();
    }

    @Test
    void serialize_serializesRecordToJson() throws Exception {
        Response<String> response = new Response<>(List.of("hola"), List.of());

        String json = serializer.serialize(response);

        assertThat(json)
                .contains("\"hola\"")
                .startsWith("{")
                .endsWith("}");
    }

    @Nested
    class LocalDateTimeSerialization {

        @Test
        void serialize_localDateTime_seSerializaComoStringIso8601NoComoArregloNiTimestamp() throws Exception {
            record Dated(LocalDateTime time, String name) {
            }
            Dated data = new Dated(LocalDateTime.of(2025, 1, 1, 10, 30), "x");

            String json = serializer.serialize(data);

            JsonNode time = JSON_PARSER.readTree(json).get("time");
            assertIso8601String(time, "2025-01-01T10:30:00");
        }

        @ParameterizedTest(name = "[{index}] {0} se serializa como {1}")
        @CsvSource({
                "2025-01-01T00:00:00, 2025-01-01T00:00:00",
                "2024-02-29T23:59:59, 2024-02-29T23:59:59",
                "2025-12-31T23:59:59, 2025-12-31T23:59:59",
                "2025-06-15T12:00:00.123456789, 2025-06-15T12:00:00.123456789"
        })
        void serialize_valoresLimite_localDateTimeSeSerializaComoIso8601(String valor, String isoEsperado) throws Exception {
            record Dated(LocalDateTime time) {
            }

            String json = serializer.serialize(new Dated(LocalDateTime.parse(valor)));

            JsonNode time = JSON_PARSER.readTree(json).get("time");
            assertIso8601String(time, isoEsperado);
        }

        private void assertIso8601String(JsonNode time, String isoEsperado) {
            assertAll(
                    () -> assertThat(time).as("el nodo time existe").isNotNull(),
                    () -> assertThat(time.isTextual()).as("time debe ser cadena JSON").isTrue(),
                    () -> assertThat(time.asText()).as("time en formato ISO-8601").isEqualTo(isoEsperado),
                    () -> assertThat(time.isArray()).as("time no debe serializarse como arreglo").isFalse(),
                    () -> assertThat(time.isNumber()).as("time no debe serializarse como timestamp numerico").isFalse()
            );
        }
    }

    @Test
    void serialize_messageDto_usesCreatedAtSnakeCaseAndUtcIsoInstant() throws Exception {
        var dto = co.edu.uco.application.primaryports.dto.message.MessageDTO.create(
                "MSG_WELCOME", "Welcome", "Hello", "info", "general", "func",
                Instant.parse("2025-01-02T03:04:05Z"));

        JsonNode json = JSON_PARSER.readTree(serializer.serialize(dto));

        assertAll(
                () -> assertThat(json.get("created_at").asText()).isEqualTo("2025-01-02T03:04:05Z"),
                () -> assertThat(json.has("createdAt")).isFalse(),
                () -> assertThat(json.has("application")).isFalse(),
                () -> assertThat(json.get("code").asText()).isEqualTo("MSG_WELCOME"),
                () -> assertThat(json.get("functionality").asText()).isEqualTo("func"));
    }

    @Test
    void serialize_messagePage_omitsApplicationAndPreservesMessageFields() throws Exception {
        var dto = co.edu.uco.application.primaryports.dto.message.MessageDTO.create(
                "MSG_LIST", "Listed", "Body", "info", "general", "catalog/functionality");
        var page = SimplePage.of(List.of(dto), 1, 10, 1, 1);

        JsonNode json = JSON_PARSER.readTree(serializer.serialize(page));
        JsonNode message = json.get("data").get(0);

        assertAll(
                () -> assertThat(message.has("application")).isFalse(),
                () -> assertThat(message.get("code").asText()).isEqualTo("MSG_LIST"),
                () -> assertThat(message.get("title").asText()).isEqualTo("Listed"),
                () -> assertThat(message.get("content").asText()).isEqualTo("Body"),
                () -> assertThat(message.get("functionality").asText()).isEqualTo("catalog/functionality"));
    }

    @Test
    void serialize_throwsCrossWordsException_onSerializationError() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("TCH_018")).thenReturn("serialization error");
        CatalogPortStaticRef.set(catalogPort);

        Object cyclic = new Object() {
            @SuppressWarnings("unused")
            public Object self = this;
        };

        assertThatThrownBy(() -> serializer.serialize(cyclic))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("serialization error"));
    }
}
