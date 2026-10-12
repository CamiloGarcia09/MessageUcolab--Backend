package co.edu.uco.application.secondaryports.event;

import java.util.concurrent.Flow;

public interface MessageEventStreamPort {
    Flow.Publisher<MessageEvent> subscribe(String environmentId);
}
