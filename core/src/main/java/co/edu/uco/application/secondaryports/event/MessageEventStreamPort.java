package co.edu.uco.application.secondaryports.event;

import java.util.Set;
import java.util.concurrent.Flow;

public interface MessageEventStreamPort {
    Flow.Publisher<MessageEvent> subscribe(String environmentId);

    // Eventos de varios ambientes, por ejemplo todos los de una aplicacion.
    Flow.Publisher<MessageEvent> subscribe(Set<String> environmentIds);

    // La aplicacion debe haber sido autorizada antes de abrir esta suscripcion.
    Flow.Publisher<MessageEvent> subscribeToApplication(String applicationId, Set<String> environmentIds);
}
