package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.helpers.UtilText;
import co.edu.uco.crosscutting.helpers.json.UtilMapperJson;
import co.edu.uco.infraestructure.config.PulsarProperties;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

/**
 * Publica en Pulsar un aviso por cada cambio de mensaje, para que los componentes consumidores
 * se enteren sin consultar el catalogo de forma repetitiva.
 *
 * <p>Aislamiento: cada aviso de un ambiente se publica en el topic de cada identidad de servicio
 * asociada a ese ambiente ({@code <prefijo>-<clientId>}). El consumidor se suscribe al topic de su
 * propio client_id, que ya conoce, y MessageUcoLab decide a quien le llega segun la tabla
 * {@code environment_service_identity}: no hay que copiar el id del ambiente y, si la identidad cambia
 * de ambiente, los avisos la siguen. Tambien se publica en el topic del ambiente
 * ({@code <prefijo>-<environmentId>}) para los consumidores que ya se suscribieron asi.</p>
 *
 * <p>Solo se publican los eventos por ambiente que emite el proyector; los eventos por aplicacion
 * mezclan ambientes y no se publican.</p>
 *
 * <p>Este publicador nunca lanza excepciones: el proyector que lo invoca no las controla y un fallo
 * del broker no debe interrumpir la proyeccion ni el SSE administrativo. Los productores se crean
 * de forma perezosa para que Pulsar no sea requisito de arranque.</p>
 */
@Component
@ConditionalOnProperty(prefix = "pulsar", name = "events-enabled", havingValue = "true", matchIfMissing = true)
public final class PulsarMessageEventPublisher implements Publisher<MessageEvent> {

    // El identificador forma parte del nombre del topic: solo se aceptan caracteres seguros.
    private static final Pattern SAFE_TOPIC_SEGMENT = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String TOPIC_SEPARATOR = "-";

    private final LoggingPort log;
    private final PulsarClient client;
    private final PulsarProperties properties;
    private final UtilMapperJson mapperJson;
    private final Clock clock;
    private final CatalogPort catalogPort;
    private final ServiceIdentityRepository identities;
    private final Map<String, CompletableFuture<Producer<String>>> producers = new ConcurrentHashMap<>();

    public PulsarMessageEventPublisher(
            final PulsarClient client,
            final PulsarProperties properties,
            final UtilMapperJson mapperJson,
            final Clock clock,
            final CatalogPort catalogPort,
            final LoggingPortFactory loggerFactory,
            final ServiceIdentityRepository identities) {
        this.log = loggerFactory.getLogger(PulsarMessageEventPublisher.class);
        this.client = client;
        this.properties = properties;
        this.mapperJson = mapperJson;
        this.clock = clock;
        this.catalogPort = catalogPort;
        this.identities = identities;
    }

    @Override
    public void publish(final MessageEvent event) {
        if (!isPublishable(event)) {
            return;
        }
        try {
            final var payload = mapperJson.execute(MessageChangedEvent.from(event, clock));
            if (payload.isEmpty()) {
                log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_039.getCode()));
                return;
            }
            topicsFor(event).forEach(topic -> send(topic, event, payload.get()));
        } catch (Exception exception) {
            logPublishFailure(event, exception);
        }
    }

    // Se exige ambiente valido. El codigo puede estar vacio en las invalidaciones colectivas.
    private static boolean isPublishable(final MessageEvent event) {
        return !isNullObject(event)
                && !UtilText.isEmpty(event.environmentId())
                && SAFE_TOPIC_SEGMENT.matcher(event.environmentId()).matches();
    }

    // Topic del ambiente y topic de cada identidad asociada al ambiente, sin repetir.
    private Set<String> topicsFor(final MessageEvent event) {
        final Set<String> topics = new LinkedHashSet<>();
        topics.add(topic(event.environmentId()));
        clientIdsOf(event.environmentId()).stream()
                .filter(clientId -> !UtilText.isEmpty(clientId) && SAFE_TOPIC_SEGMENT.matcher(clientId).matches())
                .map(this::topic)
                .forEach(topics::add);
        return topics;
    }

    // Si no se pueden leer las identidades, el aviso igual sale por el topic del ambiente.
    private List<String> clientIdsOf(final String environmentId) {
        try {
            final var clientIds = identities.findClientIdsByEnvironmentId(environmentId);
            return isNullObject(clientIds) ? List.of() : clientIds;
        } catch (RuntimeException exception) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_109.getCode()), exception);
            return List.of();
        }
    }

    private String topic(final String segment) {
        return properties.getEventsTopicPrefix() + TOPIC_SEPARATOR + segment;
    }

    private void send(final String topic, final MessageEvent event, final String payload) {
        producer(topic)
                .thenCompose(producer -> {
                    final var message = producer.newMessage().value(payload);
                    // La clave conserva el orden de los avisos de un mismo mensaje.
                    if (!UtilText.isEmpty(event.messageCode())) {
                        message.key(event.messageCode());
                    }
                    return message.sendAsync();
                })
                .whenComplete((messageId, error) -> {
                    if (!isNullObject(error)) {
                        logPublishFailure(event, error);
                    }
                });
    }

    private CompletableFuture<Producer<String>> producer(final String topic) {
        final boolean[] created = {false};
        final CompletableFuture<Producer<String>> future = producers.computeIfAbsent(topic, key -> {
            created[0] = true;
            return client.newProducer(Schema.STRING).topic(key).createAsync();
        });
        if (created[0]) {
            discardOnFailure(topic, future);
        }
        return future;
    }

    // Si Pulsar no estaba disponible se descarta el productor para reintentar en el siguiente aviso.
    // El callback se registra fuera de computeIfAbsent: un futuro ya fallido lo ejecutaria de inmediato
    // y modificaria el mapa de forma recursiva.
    private void discardOnFailure(final String topic, final CompletableFuture<Producer<String>> future) {
        future.whenComplete((producer, error) -> {
            if (!isNullObject(error)) {
                producers.remove(topic, future);
                log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_108.getCode()), error);
            }
        });
    }

    private void logPublishFailure(final MessageEvent event, final Throwable error) {
        log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_107.getCode()), event.type(), event.messageCode(), error);
    }

    @PreDestroy
    void close() {
        producers.values().forEach(future ->
                future.thenCompose(Producer::closeAsync).exceptionally(error -> null));
        producers.clear();
    }
}
