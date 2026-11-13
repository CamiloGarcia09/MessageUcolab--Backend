package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.crosscutting.helpers.json.UtilMapperJson;
import co.edu.uco.infraestructure.config.PulsarProperties;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PulsarMessageEventPublisher")
class PulsarMessageEventPublisherTest {

    private static final String PREFIX = "persistent://public/default/message-events";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC);

    @Mock PulsarClient client;
    @Mock ProducerBuilder<String> builder;
    @Mock Producer<String> producer;
    @Mock TypedMessageBuilder<String> typedMessage;
    @Mock MessageId messageId;
    @Mock CatalogPort catalogPort;
    @Mock LoggingPortFactory loggerFactory;
    @Mock LoggingPort log;
    @Mock ServiceIdentityRepository identities;

    private final UtilMapperJson mapperJson = new UtilMapperJson();
    private PulsarProperties properties;
    private PulsarMessageEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new PulsarProperties();
        properties.setEventsTopicPrefix(PREFIX);
        when(loggerFactory.getLogger(PulsarMessageEventPublisher.class)).thenReturn(log);
        when(catalogPort.getMessage("TCH_107")).thenReturn("publish-error");
        when(catalogPort.getMessage("TCH_108")).thenReturn("producer-error");
        when(catalogPort.getMessage("TCH_039")).thenReturn("serialization-error");
        when(catalogPort.getMessage("TCH_109")).thenReturn("identities-error");
        when(identities.findClientIdsByEnvironmentId(anyString())).thenReturn(List.of());
        when(client.newProducer(Schema.STRING)).thenReturn(builder);
        when(builder.topic(anyString())).thenReturn(builder);
        when(builder.createAsync()).thenReturn(CompletableFuture.completedFuture(producer));
        when(producer.newMessage()).thenReturn(typedMessage);
        when(typedMessage.value(anyString())).thenReturn(typedMessage);
        when(typedMessage.key(anyString())).thenReturn(typedMessage);
        when(typedMessage.sendAsync()).thenReturn(CompletableFuture.completedFuture(messageId));
        publisher = new PulsarMessageEventPublisher(client, properties, mapperJson, CLOCK, catalogPort, loggerFactory, identities);
    }

    private static MessageEvent environmentEvent(String type, String environmentId, String code) {
        return new MessageEvent("evt-" + environmentId, type, environmentId, code);
    }

    private MessageChangedEvent lastPayload() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(typedMessage).value(captor.capture());
        return mapperJson.execute(captor.getValue(), MessageChangedEvent.class).orElseThrow();
    }

    @Nested
    @DisplayName("publicación")
    class Publishing {

        @Test
        @DisplayName("Publica en el topic del ambiente con el contrato v1 y el código como clave")
        void publishesToEnvironmentTopic() {
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));

            verify(builder).topic(PREFIX + "-env-1");
            verify(typedMessage).key("MSG_001");
            assertThat(lastPayload()).isEqualTo(new MessageChangedEvent(
                    "evt-env-1", "MessageUpdated", 1, "env-1", "MSG_001", "2026-11-07T10:00:00Z"));
            verify(typedMessage).sendAsync();
        }

        @Test
        @DisplayName("Publica los tres tipos de cambio de mensaje")
        void publishesCreatedUpdatedAndDeleted() {
            for (String type : List.of("MessageCreated", "MessageUpdated", "MessageDeleted")) {
                publisher.publish(environmentEvent(type, "env-1", "MSG_001"));
            }

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(typedMessage, times(3)).value(captor.capture());
            assertThat(captor.getAllValues())
                    .anySatisfy(json -> assertThat(json).contains("\"eventType\":\"MessageCreated\""))
                    .anySatisfy(json -> assertThat(json).contains("\"eventType\":\"MessageUpdated\""))
                    .anySatisfy(json -> assertThat(json).contains("\"eventType\":\"MessageDeleted\""));
        }

        @Test
        @DisplayName("No envía clave cuando la invalidación colectiva no trae código")
        void collectiveChangeHasNoKey() {
            publisher.publish(environmentEvent("MessagesChanged", "env-1", ""));

            verify(typedMessage, never()).key(anyString());
            verify(typedMessage).sendAsync();
            assertThat(lastPayload().messageCode()).isEmpty();
        }

        @Test
        @DisplayName("Reutiliza el productor para el mismo ambiente")
        void reusesProducerPerEnvironment() {
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_002"));

            verify(client, times(1)).newProducer(Schema.STRING);
            verify(typedMessage, times(2)).sendAsync();
        }
    }

    @Nested
    @DisplayName("aislamiento entre ambientes")
    class EnvironmentIsolation {

        @Test
        @DisplayName("Cada ambiente publica solo en su propio topic y su aviso no menciona a otros")
        void eachEnvironmentUsesItsOwnTopic() {
            publisher.publish(environmentEvent("MessageUpdated", "env-develop", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env-prod", "MSG_001"));

            ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
            verify(builder, times(2)).topic(topics.capture());
            assertThat(topics.getAllValues())
                    .containsExactly(PREFIX + "-env-develop", PREFIX + "-env-prod");

            ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
            verify(typedMessage, times(2)).value(payloads.capture());
            assertThat(payloads.getAllValues().get(0))
                    .contains("env-develop").doesNotContain("env-prod");
            assertThat(payloads.getAllValues().get(1))
                    .contains("env-prod").doesNotContain("env-develop");
        }

        @Test
        @DisplayName("Crea un productor distinto por ambiente")
        void createsOneProducerPerEnvironment() {
            publisher.publish(environmentEvent("MessageUpdated", "env-develop", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env-prod", "MSG_001"));

            verify(client, times(2)).newProducer(Schema.STRING);
        }

        @Test
        @DisplayName("Ignora el evento por aplicación, que mezcla ambientes")
        void ignoresApplicationLevelEvent() {
            publisher.publish(new MessageEvent("evt-app", "MessageUpdated", "", "MSG_001", "app-1"));

            verifyNoInteractions(client);
        }
    }

    @Nested
    @DisplayName("topic por identidad de servicio")
    class IdentityTopics {

        @Test
        @DisplayName("Publica el aviso en el topic de cada identidad asociada al ambiente, además del topic del ambiente")
        void publishesToEachIdentityTopicOfTheEnvironment() {
            when(identities.findClientIdsByEnvironmentId("env-test")).thenReturn(List.of("svc-app-inv-test", "svc-app-otro-test"));

            publisher.publish(environmentEvent("MessageUpdated", "env-test", "MSG_001"));

            ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
            verify(builder, times(3)).topic(topics.capture());
            assertThat(topics.getAllValues()).containsExactly(
                    PREFIX + "-env-test", PREFIX + "-svc-app-inv-test", PREFIX + "-svc-app-otro-test");
            ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
            verify(typedMessage, times(3)).value(payloads.capture());
            assertThat(payloads.getAllValues()).allSatisfy(json -> assertThat(json)
                    .contains("\"environmentId\":\"env-test\"").contains("\"messageCode\":\"MSG_001\""));
        }

        @Test
        @DisplayName("Un cambio en Testing no llega al topic de la identidad de Produccion")
        void identityOfAnotherEnvironmentDoesNotReceiveTheEvent() {
            when(identities.findClientIdsByEnvironmentId("env-test")).thenReturn(List.of("svc-app-inv-test"));
            when(identities.findClientIdsByEnvironmentId("env-prod")).thenReturn(List.of("svc-app-inv-prod"));

            publisher.publish(environmentEvent("MessageUpdated", "env-test", "MSG_001"));

            verify(builder).topic(PREFIX + "-svc-app-inv-test");
            verify(builder, never()).topic(PREFIX + "-svc-app-inv-prod");
            verify(builder, never()).topic(PREFIX + "-env-prod");
        }

        @Test
        @DisplayName("Descarta client_id vacíos o con caracteres no válidos para un topic")
        void skipsUnsafeClientIds() {
            when(identities.findClientIdsByEnvironmentId("env-1")).thenReturn(List.of("svc/../otro", "", "svc ok", "svc-valido"));

            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));

            ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
            verify(builder, times(2)).topic(topics.capture());
            assertThat(topics.getAllValues()).containsExactly(PREFIX + "-env-1", PREFIX + "-svc-valido");
        }

        @Test
        @DisplayName("Si no puede leer las identidades, igual publica en el topic del ambiente y registra el error")
        void repositoryFailureStillPublishesEnvironmentTopic() {
            when(identities.findClientIdsByEnvironmentId("env-1")).thenThrow(new IllegalStateException("surreal down"));

            assertThatCode(() -> publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001")))
                    .doesNotThrowAnyException();

            verify(builder).topic(PREFIX + "-env-1");
            verify(typedMessage).sendAsync();
            verify(log).error(eq("identities-error"), any(Throwable.class));
        }

        @Test
        @DisplayName("Reutiliza el productor del topic de cada identidad")
        void reusesIdentityProducer() {
            when(identities.findClientIdsByEnvironmentId("env-1")).thenReturn(List.of("svc-inv-1"));

            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_002"));

            verify(client, times(2)).newProducer(Schema.STRING);
            verify(typedMessage, times(4)).sendAsync();
        }
    }

    @Nested
    @DisplayName("eventos no publicables")
    class NotPublishable {

        @Test
        @DisplayName("Ignora un evento nulo")
        void ignoresNullEvent() {
            assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();

            verifyNoInteractions(client);
        }

        @Test
        @DisplayName("Ignora un ambiente vacío o en blanco")
        void ignoresBlankEnvironment() {
            publisher.publish(environmentEvent("MessageUpdated", "", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "   ", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", null, "MSG_001"));

            verifyNoInteractions(client);
        }

        @Test
        @DisplayName("Rechaza identificadores de ambiente con caracteres no válidos para un topic")
        void rejectsUnsafeEnvironmentIdentifier() {
            publisher.publish(environmentEvent("MessageUpdated", "env/../other", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env 1", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env:1", "MSG_001"));

            verifyNoInteractions(client);
        }
    }

    @Nested
    @DisplayName("tolerancia a fallos")
    class FailureTolerance {

        @Test
        @DisplayName("No lanza excepción si falla la creación del productor y reintenta en el siguiente aviso")
        void producerCreationFailureDoesNotThrowAndRetries() {
            when(builder.createAsync())
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("pulsar down")))
                    .thenReturn(CompletableFuture.completedFuture(producer));

            assertThatCode(() -> publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001")))
                    .doesNotThrowAnyException();
            verify(log).error(eq("producer-error"), any(Throwable.class));
            verify(log).error(eq("publish-error"), eq("MessageUpdated"), eq("MSG_001"), any(Throwable.class));

            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_002"));

            verify(client, times(2)).newProducer(Schema.STRING);
            verify(typedMessage).sendAsync();
        }

        @Test
        @DisplayName("Descarta el productor que falla de forma asíncrona y lo recrea en el siguiente aviso")
        void asynchronousProducerFailureIsDiscarded() {
            CompletableFuture<Producer<String>> pending = new CompletableFuture<>();
            when(builder.createAsync())
                    .thenReturn(pending)
                    .thenReturn(CompletableFuture.completedFuture(producer));

            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));
            pending.completeExceptionally(new IllegalStateException("pulsar down"));
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_002"));

            verify(client, times(2)).newProducer(Schema.STRING);
            verify(log).error(eq("producer-error"), any(Throwable.class));
            verify(typedMessage).sendAsync();
        }

        @Test
        @DisplayName("No lanza excepción si falla el envío y lo registra")
        void sendFailureDoesNotThrow() {
            when(typedMessage.sendAsync())
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("send failed")));

            assertThatCode(() -> publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001")))
                    .doesNotThrowAnyException();

            verify(log).error(eq("publish-error"), eq("MessageUpdated"), eq("MSG_001"), any(Throwable.class));
        }

        @Test
        @DisplayName("No lanza excepción si el cliente falla de forma síncrona")
        void synchronousClientFailureDoesNotThrow() {
            when(client.newProducer(Schema.STRING)).thenThrow(new IllegalStateException("client closed"));

            assertThatCode(() -> publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001")))
                    .doesNotThrowAnyException();

            verify(log).error(eq("publish-error"), eq("MessageUpdated"), eq("MSG_001"), any(Throwable.class));
        }

        @Test
        @DisplayName("No envía nada y registra el error si el aviso no se puede serializar")
        void serializationFailureIsLoggedAndNothingIsSent() {
            UtilMapperJson failingMapper = org.mockito.Mockito.mock(UtilMapperJson.class);
            when(failingMapper.execute(any(Object.class))).thenReturn(Optional.empty());
            PulsarMessageEventPublisher failing = new PulsarMessageEventPublisher(
                    client, properties, failingMapper, CLOCK, catalogPort, loggerFactory, identities);

            assertThatCode(() -> failing.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001")))
                    .doesNotThrowAnyException();

            verify(log).error("serialization-error");
            verifyNoInteractions(client);
        }
    }

    @Nested
    @DisplayName("cierre")
    class Closing {

        @Test
        @DisplayName("Cierra los productores creados al apagar")
        void closesProducers() {
            when(producer.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
            publisher.publish(environmentEvent("MessageUpdated", "env-1", "MSG_001"));
            publisher.publish(environmentEvent("MessageUpdated", "env-2", "MSG_001"));

            publisher.close();

            verify(producer, times(2)).closeAsync();
        }

        @Test
        @DisplayName("Cerrar sin productores no falla")
        void closingWithoutProducersDoesNothing() {
            assertThatCode(publisher::close).doesNotThrowAnyException();

            verifyNoInteractions(client);
        }
    }
}
