package co.edu.uco.infraestructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Adapter Jackson 2 configuration")
class AdapterJsonConfigTest {

    private final ObjectMapper mapper = new AdapterJsonConfig().objectMapper();

    @Test
    @DisplayName("Preserva fechas ISO y tolera campos añadidos al leer JSON de adaptadores")
    void objectMapper_preservesAdapterJsonCompatibility() throws JsonProcessingException {
        var expected = new AdapterEvent(LocalDateTime.of(2026, 10, 20, 12, 34, 56));

        String serialized = mapper.writeValueAsString(expected);
        AdapterEvent deserialized = mapper.readValue(
                "{\"occurredAt\":\"2026-10-20T12:34:56\",\"futureField\":\"ignored\"}",
                AdapterEvent.class);

        assertAll(
                () -> assertThat(serialized).contains("\"occurredAt\":\"2026-10-20T12:34:56\""),
                () -> assertThat(serialized).doesNotContain("[2026,"),
                () -> assertThat(deserialized).isEqualTo(expected),
                () -> assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse(),
                () -> assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse(),
                () -> assertThat(mapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse());
    }

    private record AdapterEvent(LocalDateTime occurredAt) {
    }
}
