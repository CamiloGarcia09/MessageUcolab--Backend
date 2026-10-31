package co.edu.uco.messageucolab.client.api;

import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import reactor.core.publisher.Mono;

public interface MessageReader {

    Mono<MessageDetails> getDetails(String code);

    Mono<MessageDetails> getDetails(String code, String language);

    Mono<MessageCollection> getAll();
}
