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
        @DisplayName("Entrega todos los ambientes cuando el filtro es nulo")
        void nullEnvironmentReceivesEveryEvent() {
            MessageEvent first = event("id-1", "env-1");
            MessageEvent second = event("id-2", "env-2");

            StepVerifier.create(stream(null))
                    .then(() -> {
                        adapter.publish(first);
                        adapter.publish(second);
                    })
                    .expectNext(first, second)
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
}
