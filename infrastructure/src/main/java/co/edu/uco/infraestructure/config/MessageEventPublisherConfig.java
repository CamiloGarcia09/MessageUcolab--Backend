package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.infraestructure.secondaryadapters.event.CompositeMessageEventPublisher;
import co.edu.uco.infraestructure.secondaryadapters.event.InMemoryMessageEventAdapter;
import co.edu.uco.infraestructure.secondaryadapters.event.PulsarMessageEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Compone los destinos de los eventos de mensajes. El bean primario es el que recibe el proyector,
 * que depende solo de {@code Publisher<MessageEvent>}.
 */
@Configuration
public class MessageEventPublisherConfig {

    @Bean
    @Primary
    public Publisher<MessageEvent> messageEventPublisher(
            final InMemoryMessageEventAdapter inMemoryPublisher,
            final ObjectProvider<PulsarMessageEventPublisher> pulsarPublisher,
            final CatalogPort catalogPort,
            final LoggingPortFactory loggerFactory) {
        final List<Publisher<MessageEvent>> delegates = new ArrayList<>();
        delegates.add(inMemoryPublisher);
        pulsarPublisher.ifAvailable(delegates::add);
        return new CompositeMessageEventPublisher(delegates, catalogPort, loggerFactory);
    }
}
