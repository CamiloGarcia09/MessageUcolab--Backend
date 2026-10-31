package co.edu.uco.messageucolab.client.http;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabProperties;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

public final class MessageUcoLabHttpAdapter implements MessageQueryPort {

    private static final String APPLICATION_HEADER = "X-Application-Id";
    private static final String ENVIRONMENT_HEADER = "X-Environment-Id";
    private static final String MESSAGES_PATH = "/messageucolab/v1/public/messages";

    private final WebClient client;
    private final MessageUcoLabProperties properties;

    public MessageUcoLabHttpAdapter(WebClient.Builder builder, MessageUcoLabProperties properties) {
        this.client = builder.baseUrl(properties.url()).build();
        this.properties = properties;
    }

    @Override
    public Mono<MessageDetails> findByCode(String code) {
        return findByCode(code, null);
    }

    @Override
    public Mono<MessageDetails> findByCode(String code, String language) {
        return client.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(MESSAGES_PATH + "/{code}");
                    if (language != null && !language.isBlank()) {
                        uriBuilder.queryParam("language", language);
                    }
                    return uriBuilder.build(code);
                })
                .headers(this::addContextHeaders)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .timeout(properties.timeout())
                .map(MessageUcoLabHttpAdapter::toDetails)
                .onErrorMap(WebClientResponseException.class, this::toResponseException)
                .onErrorMap(this::isTransportError, MessageClientTransportException::new);
    }

    @Override
    public Mono<MessageCollection> findAll() {
        return client.get()
                .uri(MESSAGES_PATH)
                .headers(this::addContextHeaders)
                .retrieve()
                .bodyToMono(MessageListResponse.class)
                .timeout(properties.timeout())
                .map(MessageUcoLabHttpAdapter::toCollection)
                .onErrorMap(WebClientResponseException.class, this::toResponseException)
                .onErrorMap(this::isTransportError, MessageClientTransportException::new);
    }

    private void addContextHeaders(org.springframework.http.HttpHeaders headers) {
        headers.set(APPLICATION_HEADER, properties.applicationId());
        headers.set(ENVIRONMENT_HEADER, properties.environmentId());
    }

    private MessageClientResponseException toResponseException(WebClientResponseException error) {
        return new MessageClientResponseException(error.getStatusCode().value(), error);
    }

    private boolean isTransportError(Throwable error) {
        return !(error instanceof MessageClientResponseException)
                && !(error instanceof MessageClientTransportException);
    }

    private static MessageCollection toCollection(MessageListResponse response) {
        List<MessageDetails> messages = response.messages().stream()
                .map(MessageUcoLabHttpAdapter::toDetails)
                .toList();
        return new MessageCollection(response.language(), response.total(), messages);
    }

    private static MessageDetails toDetails(MessageResponse response) {
        return new MessageDetails(
                response.code(),
                response.sourceLanguage(),
                response.targetLanguage(),
                response.title(),
                response.content(),
                response.type(),
                response.category(),
                response.functionality(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
