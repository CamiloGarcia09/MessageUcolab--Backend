package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.List;

/**
 * Entrega cada evento a todos los destinos (SSE administrativo, broker) de forma independiente:
 * si uno falla, los demas siguen recibiendolo y el proyector no se ve afectado.
 */
public final class CompositeMessageEventPublisher implements Publisher<MessageEvent> {

    private final List<Publisher<MessageEvent>> delegates;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public CompositeMessageEventPublisher(
            final List<Publisher<MessageEvent>> delegates,
            final CatalogPort catalogPort,
            final LoggingPortFactory loggerFactory) {
        this.delegates = List.copyOf(delegates);
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(CompositeMessageEventPublisher.class);
    }

    @Override
    public void publish(final MessageEvent event) {
        for (final Publisher<MessageEvent> delegate : delegates) {
            try {
                delegate.publish(event);
            } catch (RuntimeException exception) {
                log.error(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_109.getCode()),
                        delegate.getClass().getSimpleName(), exception);
            }
        }
    }
}
