package co.edu.uco.messageucolab.client.api.port;

import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import reactor.core.publisher.Mono;

public interface MessageQueryPort {

    Mono<MessageDetails> findByCode(String code);

    Mono<MessageDetails> findByCode(String code, String language);

    Mono<MessageCollection> findAll();
}
