package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.event.MessageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageChangedEvent")
class MessageChangedEventTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Construye el aviso desde el evento por ambiente con versión de esquema y fecha UTC")
    void fromMapsEnvironmentEvent() {
        MessageEvent source = new MessageEvent("evt-1", "MessageUpdated", "env-1", "MSG_001");

        MessageChangedEvent event = MessageChangedEvent.from(source, CLOCK);

        assertThat(event).isEqualTo(new MessageChangedEvent(
                "evt-1", "MessageUpdated", 1, "env-1", "MSG_001", "2026-11-07T10:00:00Z"));
    }

    @Test
    @DisplayName("La versión de esquema actual es 1")
    void currentSchemaVersionIsOne() {
        assertThat(MessageChangedEvent.CURRENT_SCHEMA_VERSION).isEqualTo(1);
    }

    @Test
    @DisplayName("Conserva el código vacío de las invalidaciones colectivas")
    void keepsEmptyCodeForCollectiveChanges() {
        MessageChangedEvent event = MessageChangedEvent.from(
                new MessageEvent("evt-2", "MessagesChanged", "env-1", ""), CLOCK);

        assertThat(event.messageCode()).isEmpty();
        assertThat(event.eventType()).isEqualTo("MessagesChanged");
    }
}
