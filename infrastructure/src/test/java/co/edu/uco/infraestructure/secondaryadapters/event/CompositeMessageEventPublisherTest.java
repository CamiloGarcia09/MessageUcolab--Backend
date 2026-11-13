package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CompositeMessageEventPublisher")
class CompositeMessageEventPublisherTest {

    @Mock Publisher<MessageEvent> first;
    @Mock Publisher<MessageEvent> second;
    @Mock CatalogPort catalogPort;
    @Mock LoggingPortFactory loggerFactory;
    @Mock LoggingPort log;

    private final MessageEvent event = new MessageEvent("evt-1", "MessageUpdated", "env-1", "MSG_001");

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CompositeMessageEventPublisher.class)).thenReturn(log);
        when(catalogPort.getMessage("TCH_109")).thenReturn("delegate-error");
    }

    private CompositeMessageEventPublisher composite(List<Publisher<MessageEvent>> delegates) {
        return new CompositeMessageEventPublisher(delegates, catalogPort, loggerFactory);
    }

    @Test
    @DisplayName("Entrega el evento a todos los destinos en orden")
    void deliversToAllDelegatesInOrder() {
        composite(List.of(first, second)).publish(event);

        InOrder order = inOrder(first, second);
        order.verify(first).publish(event);
        order.verify(second).publish(event);
    }

    @Test
    @DisplayName("Si un destino falla, los demás siguen recibiendo el evento y se registra el fallo")
    void failingDelegateDoesNotStopTheOthers() {
        doThrow(new IllegalStateException("broker down")).when(first).publish(event);

        assertThatCode(() -> composite(List.of(first, second)).publish(event)).doesNotThrowAnyException();

        verify(second).publish(event);
        verify(log).error(eq("delegate-error"), anyString(), any(RuntimeException.class));
    }

    @Test
    @DisplayName("No registra errores cuando todos los destinos funcionan")
    void doesNotLogWhenAllSucceed() {
        composite(List.of(first, second)).publish(event);

        verify(log, never()).error(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Funciona con un solo destino, por ejemplo cuando Pulsar está desactivado")
    void worksWithSingleDelegate() {
        composite(List.of(first)).publish(event);

        verify(first).publish(event);
        verify(second, never()).publish(any());
    }

    @Test
    @DisplayName("No se ve afectado si se modifica la lista original después de construirlo")
    void ignoresLaterChangesToTheSourceList() {
        List<Publisher<MessageEvent>> source = new ArrayList<>(List.of(first));
        CompositeMessageEventPublisher composite = composite(source);
        source.add(second);

        composite.publish(event);

        verify(first).publish(event);
        verify(second, never()).publish(any());
    }
}
