package co.edu.uco.messageucolab.client.api.port;

import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import reactor.core.publisher.Mono;

public interface MessageFallbackPort {

    Mono<MessageDetails> findByCode(String code);
}
