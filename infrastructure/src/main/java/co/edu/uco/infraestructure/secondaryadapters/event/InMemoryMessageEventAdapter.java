package co.edu.uco.infraestructure.secondaryadapters.event;

import co.edu.uco.application.secondaryports.event.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import java.util.concurrent.Flow;

@Component
public final class InMemoryMessageEventAdapter implements Publisher<MessageEvent>, MessageEventStreamPort {
    private final Sinks.Many<MessageEvent> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);
    public void publish(MessageEvent event) { sink.tryEmitNext(event); }
    public Flow.Publisher<MessageEvent> subscribe(String environmentId) {
        return subscriber -> {
            reactor.core.publisher.BaseSubscriber<MessageEvent> bridge = new reactor.core.publisher.BaseSubscriber<>() {
                protected void hookOnNext(MessageEvent event) { subscriber.onNext(event); }
                protected void hookOnComplete() { subscriber.onComplete(); }
                protected void hookOnError(Throwable throwable) { subscriber.onError(throwable); }
            };
            subscriber.onSubscribe(new Flow.Subscription() {
                public void request(long count) { bridge.request(count); }
                public void cancel() { bridge.cancel(); }
            });
            sink.asFlux()
                    .filter(event -> environmentId != null && environmentId.equals(event.environmentId()))
                    .subscribe(bridge);
        };
    }
}
