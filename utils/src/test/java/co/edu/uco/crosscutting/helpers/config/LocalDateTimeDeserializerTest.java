package co.edu.uco.crosscutting.helpers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDateTimeDeserializerTest {

    private final LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer();

    @Test
    void serialize_formatsDateTimeInIsoFormat() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        JsonPrimitive json = (JsonPrimitive) deserializer.serialize(dateTime, LocalDateTime.class, null);
        assertThat(json.getAsString()).isEqualTo("2023-06-15T10:30:00Z");
    }

    @Test
    void deserialize_normalizesOffsetDateTimeToUtc() {
        JsonPrimitive json = new JsonPrimitive("2023-06-15T10:30:00-05:00");

        LocalDateTime result = deserializer.deserialize(json, LocalDateTime.class, null);

        assertThat(result).isEqualTo(LocalDateTime.of(2023, 6, 15, 15, 30, 0));
    }

    @Test
    void deserialize_parsesIsoDateTime() {
        JsonPrimitive json = new JsonPrimitive("2023-06-15T10:30:00");
        LocalDateTime result = deserializer.deserialize(json, LocalDateTime.class, null);
        assertThat(result).isEqualTo(LocalDateTime.of(2023, 6, 15, 10, 30, 0));
    }

    @Test
    void deserialize_throwsCrossWordsException_forInvalidText() {
        JsonPrimitive json = new JsonPrimitive("invalid");

        assertThatThrownBy(() -> deserializer.deserialize(json, LocalDateTime.class, null))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(exception -> assertThat(((CrossWordsException) exception).getTechnicalMessage())
                        .isEqualTo("La fecha que se convertirá no tiene un formato válido."));
    }

    @Test
    void registeredInGson_roundTripsLocalDateTime() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .create();
        LocalDateTime original = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        String json = gson.toJson(original);
        LocalDateTime result = gson.fromJson(json, LocalDateTime.class);
        assertThat(json).isEqualTo("\"2023-06-15T10:30:00Z\"");
        assertThat(result).isEqualTo(original);
    }
}
