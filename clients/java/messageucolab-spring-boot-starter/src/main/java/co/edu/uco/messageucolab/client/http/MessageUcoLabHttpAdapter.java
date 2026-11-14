package co.edu.uco.messageucolab.client.http;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabProperties;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Consulta las rutas de consumo de MessageUcoLab con el Bearer de la identidad de servicio de la aplicación.
 * El PEP de MessageUcoLab solo deja pasar identidades a las que MessageUcoLab concedió esas rutas, y
 * MessageUcoLab saca la aplicación y el ambiente del client_id del token: no se envían identificadores.
 */
public final class MessageUcoLabHttpAdapter implements MessageQueryPort {

    private static final String CONSUMER_PATH = "/messageucolab/v1/messages/consumer";
    private static final String TRANSLATIONS_PATH = CONSUMER_PATH + "/translations";

    private final WebClient client;
    private final MessageUcoLabProperties properties;
    private final MessageUcoLabAccessTokenProvider tokenProvider;

    public MessageUcoLabHttpAdapter(WebClient.Builder builder, MessageUcoLabProperties properties,
                                    MessageUcoLabAccessTokenProvider tokenProvider) {
        this.client = builder.baseUrl(properties.url()).build();
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<MessageDetails> findByCode(String code) {
        return findByCode(code, null);
    }

    /**
     * Sin idioma entrega el texto original; con idioma, la traducción de MessageUcoLab.
     */
    @Override
    public Mono<MessageDetails> findByCode(String code, String language) {
        boolean translate = language != null && !language.isBlank();
        return tokenProvider.token()
                .flatMap(token -> {
                    if (!translate) {
                        return client.get()
                                .uri(uriBuilder -> uriBuilder.path(CONSUMER_PATH).queryParam("code", code).build())
                                .headers(headers -> headers.setBearerAuth(token))
                                .retrieve()
                                .bodyToMono(MessageResponse.class);
                    }
                    return client.post()
                            .uri(uriBuilder -> uriBuilder.path(TRANSLATIONS_PATH).queryParam("code", code).build())
                            .headers(headers -> headers.setBearerAuth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("targetLanguage", language))
                            .retrieve()
                            .bodyToMono(MessageResponse.class);
                })
                .timeout(properties.timeout())
                .map(MessageUcoLabHttpAdapter::toDetails)
                .onErrorMap(WebClientResponseException.class, this::toResponseException)
                .onErrorMap(this::isTransportError, MessageClientTransportException::new);
    }

    @Override
    public Mono<MessageCollection> findAll() {
        return tokenProvider.token()
                .flatMap(token -> client.get()
                        .uri(CONSUMER_PATH)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(MessageListResponse.class))
                .timeout(properties.timeout())
                .map(MessageUcoLabHttpAdapter::toCollection)
                .onErrorMap(WebClientResponseException.class, this::toResponseException)
                .onErrorMap(this::isTransportError, MessageClientTransportException::new);
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
