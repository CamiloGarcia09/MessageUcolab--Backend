package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.event.MessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

@DisplayName("InMemoryMessageEventAdapter")
class InMemoryMessageEventAdapterTest {

    private InMemoryMessageEventAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryMessageEventAdapter();
    }

    private Flux<MessageEvent> stream(String environmentId) {
        return Flux.from(org.reactivestreams.FlowAdapters.toPublisher(adapter.subscribe(environmentId)));
    }

    private MessageEvent event(String id, String environmentId) {
        return new MessageEvent(id, "MessageCreated", environmentId, "CODE");
    }

    @Nested
    @DisplayName("publish / subscribe")
    class PublishAndSubscribe {

        @Test
        @DisplayName("Entrega el evento a un suscriptor del mismo ambiente")
        void subscriberReceivesEventForMatchingEnvironment() {
            MessageEvent published = event("id-1", "env-1");

            StepVerifier.create(stream("env-1"))
                    .then(() -> adapter.publish(published))
                    .expectNext(published)
                    .thenCancel()
                    .verify();
        }

        @Test
        @DisplayName("No entrega eventos de un ambiente distinto")
        void subscriberDoesNotReceiveEventsFromOtherEnvironments() {
            MessageEvent fromOtherEnvironment = event("id-2", "env-2");

            StepVerifier.create(stream("env-1"))
                    .then(() -> adapter.publish(fromOtherEnvironment))
                    .expectNoEvent(Duration.ofMillis(200))
                    .thenCancel()
                    .verify();
        }

        @Test
        @DisplayName("No entrega eventos cuando el filtro de ambiente es nulo")
        void nullEnvironmentDoesNotReceiveEvents() {
            MessageEvent first = event("id-1", "env-1");
            MessageEvent second = event("id-2", "env-2");

            StepVerifier.create(stream(null))
                    .then(() -> {
                        adapter.publish(first);
                        adapter.publish(second);
                    })
                    .expectNoEvent(Duration.ofMillis(200))
                    .thenCancel()
                    .verify();
        }

        @Test
        @DisplayName("Distribuye el mismo evento a múltiples suscriptores del ambiente")
        void multipleSubscribersReceiveTheSameEvent() {
            MessageEvent published = event("id-1", "env-1");

            StepVerifier.create(stream("env-1").take(1))
                    .then(() -> adapter.publish(published))
                    .expectNext(published)
                    .verifyComplete();

            StepVerifier.create(stream("env-1").take(1))
                    .then(() -> adapter.publish(published))
                    .expectNext(published)
                    .verifyComplete();
        }
    }

    @org.junit.jupiter.api.Test
    void subscribeToEnvironmentSet_receivesOnlyEventsOfThoseEnvironments() {
        var adapter = new InMemoryMessageEventAdapter();
        var events = Flux.from(org.reactivestreams.FlowAdapters.toPublisher(
                adapter.subscribe(java.util.Set.of("env-dev", "env-prod"))));

        StepVerifier.create(events.take(2))
                .then(() -> {
                    adapter.publish(new MessageEvent("1", "MessageCreated", "env-dev", "A"));
                    adapter.publish(new MessageEvent("2", "MessageCreated", "env-qa", "B"));
                    adapter.publish(new MessageEvent("3", "MessageCreated", "env-prod", "C"));
                })
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event.id()).isEqualTo("1"))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event.id()).isEqualTo("3"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Aísla suscripciones por aplicación aunque compartan ambiente")
    void subscribeToApplication_isolatesApplicationsSharingEnvironment() {
        var events = Flux.from(org.reactivestreams.FlowAdapters.toPublisher(
                adapter.subscribeToApplication("app-a", java.util.Set.of("env-1"))));
        MessageEvent forOtherApp = new MessageEvent("other", "MessagesChanged", "", "", "app-b");
        MessageEvent forSameApp = new MessageEvent("same", "MessagesChanged", "", "", "app-a");

        StepVerifier.create(events.take(1))
                .then(() -> {
                    adapter.publish(forOtherApp);
                    adapter.publish(forSameApp);
                })
                .expectNext(forSameApp)
                .verifyComplete();
    }

    @Test
    @DisplayName("No entrega eventos sin aplicación ni ambiente autorizado")
    void subscribeToApplication_doesNotLeakEventsWithoutMatchingScope() {
        var events = Flux.from(org.reactivestreams.FlowAdapters.toPublisher(
                adapter.subscribeToApplication("app-a", java.util.Set.of("env-1"))));

        StepVerifier.create(events)
                .then(() -> {
                    adapter.publish(new MessageEvent("unknown", "MessagesChanged", "", ""));
                    adapter.publish(new MessageEvent("wrong-env", "MessageUpdated", "env-2", "CODE"));
                })
                .expectNoEvent(Duration.ofMillis(50))
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("No acepta una aplicación vacía para suscripción")
    void subscribeToApplication_emptyApplicationNeverReceivesEvents() {
        var events = Flux.from(org.reactivestreams.FlowAdapters.toPublisher(
                adapter.subscribeToApplication(" ", java.util.Set.of("env-1"))));

        StepVerifier.create(events)
                .then(() -> adapter.publish(new MessageEvent("id", "MessageUpdated", "env-1", "CODE", "app-a")))
                .expectNoEvent(Duration.ofMillis(50))
                .thenCancel()
                .verify();
    }
}
