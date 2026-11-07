package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.event.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.adapter.JdkFlowAdapter;
import java.util.Set;
import java.util.concurrent.Flow;

@Component
public final class InMemoryMessageEventAdapter implements Publisher<MessageEvent>, MessageEventStreamPort {
    private final Sinks.Many<MessageEvent> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);
    public void publish(MessageEvent event) { sink.tryEmitNext(event); }
    public Flow.Publisher<MessageEvent> subscribe(String environmentId) {
        return subscribe(environmentId == null ? Set.of() : Set.of(environmentId));
    }
    public Flow.Publisher<MessageEvent> subscribe(Set<String> environmentIds) {
        Set<String> filter = environmentIds == null ? Set.of() : Set.copyOf(environmentIds);
        return JdkFlowAdapter.publisherToFlowPublisher(sink.asFlux()
                .filter(event -> event.environmentId() != null && filter.contains(event.environmentId())));
    }

    @Override
    public Flow.Publisher<MessageEvent> subscribeToApplication(String applicationId, Set<String> environmentIds) {
        Set<String> filter = environmentIds == null ? Set.of() : Set.copyOf(environmentIds);
        return JdkFlowAdapter.publisherToFlowPublisher(sink.asFlux()
                .filter(event -> belongsToApplication(event, applicationId, filter)));
    }

    private static boolean belongsToApplication(MessageEvent event, String applicationId, Set<String> environments) {
        if (applicationId == null || applicationId.isBlank()) {
            return false;
        }
        if (event.applicationId() != null && !event.applicationId().isBlank()) {
            return applicationId.equals(event.applicationId());
        }
        return event.environmentId() != null && environments.contains(event.environmentId());
    }
}
