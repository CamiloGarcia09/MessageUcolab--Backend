package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.infraestructure.secondaryadapters.event.CompositeMessageEventPublisher;
import co.edu.uco.infraestructure.secondaryadapters.event.InMemoryMessageEventAdapter;
import co.edu.uco.infraestructure.secondaryadapters.event.PulsarMessageEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Primary;
import co.edu.uco.application.secondaryports.event.MessageEventStreamPort;
import java.util.Set;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MessageEventPublisherConfig")
class MessageEventPublisherConfigTest {

    @Mock CatalogPort catalogPort;
    @Mock LoggingPortFactory loggerFactory;
    @Mock LoggingPort log;

    private final MessageEventPublisherConfig config = new MessageEventPublisherConfig();
    private final MessageEvent event = new MessageEvent("evt-1", "MessageUpdated", "env-1", "MSG_001");

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CompositeMessageEventPublisher.class)).thenReturn(log);
    }

    @Test
    @DisplayName("Expone el publicador compuesto como bean primario")
    void compositeIsThePrimaryBean() throws NoSuchMethodException {
        var method = MessageEventPublisherConfig.class.getMethod("messageEventPublisher",
                InMemoryMessageEventAdapter.class,
                org.springframework.beans.factory.ObjectProvider.class,
                CatalogPort.class,
                LoggingPortFactory.class);

        assertThat(method.isAnnotationPresent(Primary.class)).isTrue();
        assertThat(method.getReturnType()).isEqualTo(Publisher.class);
    }

    @Test
    @DisplayName("Con Pulsar activo entrega el evento al SSE en memoria y a Pulsar")
    void deliversToInMemoryAndPulsarWhenAvailable() {
        InMemoryMessageEventAdapter inMemory = new InMemoryMessageEventAdapter();
        PulsarMessageEventPublisher pulsar = mock(PulsarMessageEventPublisher.class);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("pulsar", pulsar);

        Publisher<MessageEvent> publisher = config.messageEventPublisher(
                inMemory, beans.getBeanProvider(PulsarMessageEventPublisher.class), catalogPort, loggerFactory);

        StepVerifier.create(Flux.from(org.reactivestreams.FlowAdapters.toPublisher(inMemory.subscribe("env-1"))))
                .then(() -> publisher.publish(event))
                .expectNext(event)
                .thenCancel()
                .verify();
        verify(pulsar).publish(event);
    }

    @Test
    @DisplayName("Con Pulsar desactivado solo alimenta el SSE en memoria")
    void deliversOnlyToInMemoryWhenPulsarIsDisabled() {
        InMemoryMessageEventAdapter inMemory = new InMemoryMessageEventAdapter();
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

        Publisher<MessageEvent> publisher = config.messageEventPublisher(
                inMemory, beans.getBeanProvider(PulsarMessageEventPublisher.class), catalogPort, loggerFactory);

        StepVerifier.create(Flux.from(org.reactivestreams.FlowAdapters.toPublisher(inMemory.subscribe("env-1"))))
                .then(() -> publisher.publish(event))
                .expectNext(event)
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("Con el cableado real de Spring, el publicador primario alimenta el mismo canal SSE que consume el controlador")
    void springWiring_primaryPublisherFeedsTheStreamPortUsedByTheSseController() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CatalogPort.class, () -> catalogPort);
            context.registerBean(LoggingPortFactory.class, () -> loggerFactory);
            context.register(InMemoryMessageEventAdapter.class, MessageEventPublisherConfig.class);
            context.refresh();

            // Lo que recibe el proyector (por tipo, resuelto por @Primary) y lo que usa el controlador del SSE.
            @SuppressWarnings("unchecked")
            Publisher<MessageEvent> projectorPublisher = context.getBean(Publisher.class);
            MessageEventStreamPort controllerStream = context.getBean(MessageEventStreamPort.class);
            MessageEvent applicationEvent = new MessageEvent("evt-app", "MessageCreated", "", "MSG_001", "app-1");

            assertThat(projectorPublisher).isInstanceOf(CompositeMessageEventPublisher.class);
            StepVerifier.create(Flux.from(org.reactivestreams.FlowAdapters.toPublisher(
                            controllerStream.subscribeToApplication("app-1", Set.of("env-1")))))
                    .then(() -> projectorPublisher.publish(applicationEvent))
                    .expectNext(applicationEvent)
                    .thenCancel()
                    .verify();
        }
    }
}
