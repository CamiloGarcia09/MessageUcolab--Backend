package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.*;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.message.*;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.MessageEventStreamPort;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.usecase.domain.security.*;
import co.edu.uco.infraestructure.primaryadapters.MessagesController;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;
import java.util.List;
import java.time.Duration;
import java.util.UUID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

@RestController
final class MessagesControllerImpl implements MessagesController {
    private final FindMessagesByEnvironmentUsecaseFacade findPage;
    private final FindMessageByCodeAndEnvironmentUseCaseFacade findOne;
    private final TranslateMessageByCodeAndEnvironmentUseCaseFacade translate;
    private final PresenterPort<MessageDTO> presenter;
    private final PresenterPort<TranslatedMessageDTO> translationPresenter;
    private final PresenterPort<SimplePage<MessageDTO>> pagePresenter;
    private final MessageEventStreamPort eventStream;

    MessagesControllerImpl(FindMessagesByEnvironmentUsecaseFacade findPage,
                            FindMessageByCodeAndEnvironmentUseCaseFacade findOne,
                            TranslateMessageByCodeAndEnvironmentUseCaseFacade translate,
                            PresenterPort<MessageDTO> presenter,
                            PresenterPort<TranslatedMessageDTO> translationPresenter,
                            PresenterPort<SimplePage<MessageDTO>> pagePresenter,
                            MessageEventStreamPort eventStream) {
        this.findPage = findPage; this.findOne = findOne; this.translate = translate;
        this.presenter = presenter; this.translationPresenter = translationPresenter; this.pagePresenter = pagePresenter;
        this.eventStream = eventStream;
    }
    public Flux<ServerSentEvent<MessageEvent>> events(ServerWebExchange exchange) {
        String environment = (String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE);
        Flux<ServerSentEvent<MessageEvent>> events = Flux.<ServerSentEvent<MessageEvent>>create(emitter -> eventStream.subscribe(environment).subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                emitter.onDispose(subscription::cancel);
                subscription.request(Long.MAX_VALUE);
            }
            public void onNext(MessageEvent event) {
                emitter.next(ServerSentEvent.<MessageEvent>builder(event).id(event.id()).event(event.type()).build());
            }
            public void onError(Throwable throwable) { emitter.error(throwable); }
            public void onComplete() { emitter.complete(); }
        }));
        Flux<ServerSentEvent<MessageEvent>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(value -> {
                    MessageEvent event = new MessageEvent(UUID.randomUUID().toString(), "heartbeat",
                            environment, "");
                    return ServerSentEvent.<MessageEvent>builder(event).id(event.id()).event(event.type()).build();
                });
        return Flux.merge(events, heartbeat);
    }
    public Mono<Void> findByEnvironmentAndMessage(String page, String size, String sort, String columnSort,
                                                   ServerWebExchange exchange) {
        var request = PageRequestDTO.builder().page(page).size(size).sort(sort).columnSort(columnSort).build();
        return ReactiveHttpResponse.blocking(() -> findPage.execute(context(exchange), request))
                .flatMap(value -> ReactiveHttpResponse.present(pagePresenter, List.of(value), exchange));
    }
    public Mono<Void> findByCodeMessageAndEnvironment(String code, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> findOne.execute(code, context(exchange)))
                .flatMap(value -> ReactiveHttpResponse.present(presenter, List.of(value), exchange));
    }
    public Mono<Void> translateByCodeMessageAndEnvironment(String code, TranslateMessageDTO dto,
                                                            ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> translate.execute(code, context(exchange),
                        dto.getSourceLanguage(), dto.getTargetLanguage()))
                .flatMap(value -> ReactiveHttpResponse.present(translationPresenter, List.of(value), exchange));
    }
    private MessageAccessContext context(ServerWebExchange exchange) {
        return new MessageAccessContext((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE),
                (ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE));
    }
}
