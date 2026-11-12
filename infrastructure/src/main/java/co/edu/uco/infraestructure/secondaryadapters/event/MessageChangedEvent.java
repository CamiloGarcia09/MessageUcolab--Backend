package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.event.MessageEvent;

import java.time.Clock;

/**
 * Contrato versionado del aviso que se publica en el broker para los componentes consumidores.
 *
 * <p>Es independiente de {@link MessageEvent}, que alimenta el SSE administrativo, para que ambos
 * contratos evolucionen por separado. El aviso no incluye contenido: el consumidor consulta el
 * mensaje por los endpoints de consumo.</p>
 *
 * <p>Cada aviso pertenece a un solo ambiente y se publica en el topic de ese ambiente, de modo que
 * un consumidor nunca recibe avisos de otros ambientes. {@code messageCode} puede venir vacio en las
 * invalidaciones colectivas ({@code MessagesChanged}); en ese caso el consumidor debe refrescar todo
 * su ambiente.</p>
 */
public record MessageChangedEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        String environmentId,
        String messageCode,
        String occurredAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static MessageChangedEvent from(final MessageEvent event, final Clock clock) {
        return new MessageChangedEvent(
                event.id(),
                event.type(),
                CURRENT_SCHEMA_VERSION,
                event.environmentId(),
                event.messageCode(),
                clock.instant().toString());
    }
}
