package co.edu.uco.application.secondaryports.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("MessageEvent")
class MessageEventTest {

    @Test
    @DisplayName("Expone id, tipo, ambiente y código de mensaje")
    void exposesEventAttributes() {
        MessageEvent event = new MessageEvent("id-1", "MessageCreated", "env-1", "CODE");

        assertAll(
                () -> assertThat(event.id()).isEqualTo("id-1"),
                () -> assertThat(event.type()).isEqualTo("MessageCreated"),
                () -> assertThat(event.environmentId()).isEqualTo("env-1"),
                () -> assertThat(event.messageCode()).isEqualTo("CODE"));
    }

    @Test
    @DisplayName("Es un valor inmutable comparable por contenido")
    void equalityIsValueBased() {
        MessageEvent event = new MessageEvent("id-1", "MessageCreated", "env-1", "CODE");

        assertThat(event)
                .isEqualTo(new MessageEvent("id-1", "MessageCreated", "env-1", "CODE"))
                .hasSameHashCodeAs(new MessageEvent("id-1", "MessageCreated", "env-1", "CODE"))
                .isNotEqualTo(new MessageEvent("id-2", "MessageCreated", "env-1", "CODE"));
    }
}
