package co.edu.uco.messageucolab.client.api;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import co.edu.uco.messageucolab.client.api.port.MessageFallbackPort;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import reactor.core.publisher.Mono;

public final class DefaultMessageReader implements MessageReader {

    private final MessageQueryPort queryPort;
    private final MessageFallbackPort fallbackPort;

    public DefaultMessageReader(MessageQueryPort queryPort, MessageFallbackPort fallbackPort) {
        this.queryPort = queryPort;
        this.fallbackPort = fallbackPort;
    }

    @Override
    public Mono<MessageDetails> getDetails(String code) {
        return resolve(code);
    }

    @Override
    public Mono<MessageDetails> getDetails(String code, String language) {
        return queryPort.findByCode(code, language)
                .onErrorResume(this::allowsFallback, error -> fallback(code, error));
    }

    @Override
    public Mono<MessageCollection> getAll() {
        return queryPort.findAll();
    }

    private Mono<MessageDetails> resolve(String code) {
        return queryPort.findByCode(code)
                .onErrorResume(this::allowsFallback, error -> fallback(code, error));
    }

    private Mono<MessageDetails> fallback(String code, Throwable originalError) {
        return fallbackPort.findByCode(code)
                .switchIfEmpty(Mono.error(originalError));
    }

    private boolean allowsFallback(Throwable error) {
        if (error instanceof MessageClientTransportException) {
            return true;
        }
        return error instanceof MessageClientResponseException response
                && (response.getStatusCode() == 404 || response.getStatusCode() >= 500);
    }

}
