package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslateMessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndEnvironmentUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByEnvironmentUsecaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndEnvironmentUseCaseFacade;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.MessageEventStreamPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.SubmissionPublisher;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagesControllerImpl")
class MessagesControllerImplTest {

    private static final ExternalIdentity IDENTITY = new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);

    @Mock
    private FindMessagesByEnvironmentUsecaseFacade findPage;
    @Mock
    private FindMessageByCodeAndEnvironmentUseCaseFacade findOne;
    @Mock
    private TranslateMessageByCodeAndEnvironmentUseCaseFacade translate;
    @Mock
    private PresenterPort<MessageDTO> presenter;
    @Mock
    private PresenterPort<TranslatedMessageDTO> translationPresenter;
    @Mock
    private PresenterPort<SimplePage<MessageDTO>> pagePresenter;
    @Mock
    private MessageEventStreamPort eventStream;
    @Mock
    private CatalogPort catalog;

    private MessagesControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new MessagesControllerImpl(findPage, findOne, translate, presenter, translationPresenter,
                pagePresenter, eventStream, catalog);
    }

    private MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages").build());
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, "env-1");
        return exchange;
    }

    private void stubPageRender() {
        when(pagePresenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    private void stubMessageRender() {
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    private void stubTranslationRender() {
        when(translationPresenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    @Nested
    @DisplayName("findByEnvironmentAndMessage")
    class FindByEnvironmentAndMessage {

        @Test
        @DisplayName("Construye el PageRequest y presenta la página con el ambiente del token")
        void buildsPageRequestAndPresents() {
            stubPageRender();
            SimplePage<MessageDTO> page = SimplePage.of(List.of(), 1, 10, 0, 0);
            when(findPage.execute(any(MessageAccessContext.class), any(PageRequestDTO.class))).thenReturn(page);

            StepVerifier.create(controller.findByEnvironmentAndMessage(
                            "1", "10", "asc", "code",
                            null, null, null, null, null, null,
                            exchange()))
                    .verifyComplete();

            ArgumentCaptor<PageRequestDTO> requestCaptor = ArgumentCaptor.forClass(PageRequestDTO.class);
            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(findPage).execute(contextCaptor.capture(), requestCaptor.capture());
            PageRequestDTO captured = requestCaptor.getValue();
            assertAll(
                    () -> assertThat(contextCaptor.getValue().workspaceEnvironmentId()).isEqualTo("env-1"),
                    () -> assertThat(contextCaptor.getValue().externalIdentity()).isNull(),
                    () -> assertThat(captured.getPage()).isEqualTo("1"),
                    () -> assertThat(captured.getSize()).isEqualTo("10"),
                    () -> assertThat(captured.getSort()).isEqualTo("asc"),
                    () -> assertThat(captured.getColumnSort()).isEqualTo("code"));
            verify(pagePresenter).presentRestSuccess(List.of(page), null);
        }
    }

    @Nested
    @DisplayName("findByCodeMessageAndEnvironment")
    class FindByCodeMessageAndEnvironment {

        @Test
        @DisplayName("Delega con el código y presenta el mensaje encontrado")
        void delegatesAndPresents() {
            stubMessageRender();
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "TYPE", "CAT", "FUNC");
            when(findOne.execute(eq("CODE"), any(MessageAccessContext.class))).thenReturn(dto);

            StepVerifier.create(controller.findByCodeMessageAndEnvironment("CODE", exchange())).verifyComplete();

            verify(findOne).execute(eq("CODE"), any(MessageAccessContext.class));
            verify(presenter).presentRestSuccess(List.of(dto), null);
        }

        @Test
        @DisplayName("Propaga la identidad externa junto con el ambiente del token")
        void propagatesExternalIdentityAlongsideEnvironment() {
            stubMessageRender();
            MockServerWebExchange exchange = exchange();
            exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "TYPE", "CAT", "FUNC");
            when(findOne.execute(eq("CODE"), any(MessageAccessContext.class))).thenReturn(dto);

            StepVerifier.create(controller.findByCodeMessageAndEnvironment("CODE", exchange)).verifyComplete();

            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(findOne).execute(eq("CODE"), contextCaptor.capture());
            assertThat(contextCaptor.getValue())
                    .extracting(MessageAccessContext::workspaceEnvironmentId, MessageAccessContext::externalIdentity)
                    .containsExactly("env-1", IDENTITY);
        }
    }

    @Nested
    @DisplayName("translateByCodeMessageAndEnvironment")
    class TranslateByCodeMessageAndEnvironment {

        @Test
        @DisplayName("Delega con los idiomas origen y destino y presenta la traducción")
        void delegatesWithSourceAndTargetLanguages() {
            stubTranslationRender();
            TranslatedMessageDTO dto = translatedMessage("es", "en");
            TranslateMessageDTO body = new TranslateMessageDTO();
            body.setSourceLanguage("es");
            body.setTargetLanguage("en");
            when(translate.execute(eq("CODE"), any(MessageAccessContext.class), eq("es"), eq("en"))).thenReturn(dto);

            StepVerifier.create(controller.translateByCodeMessageAndEnvironment("CODE", body, exchange()))
                    .verifyComplete();

            verify(translate).execute(eq("CODE"), any(MessageAccessContext.class), eq("es"), eq("en"));
            verify(translationPresenter).presentRestSuccess(List.of(dto), null);
        }

        @Test
        @DisplayName("Usa idioma origen automático cuando el cuerpo no lo especifica")
        void usesAutomaticSourceLanguageWhenOmitted() {
            stubTranslationRender();
            TranslatedMessageDTO dto = translatedMessage("auto", "en");
            TranslateMessageDTO body = new TranslateMessageDTO();
            body.setTargetLanguage("en");
            when(translate.execute(eq("CODE"), any(MessageAccessContext.class), eq("auto"), eq("en"))).thenReturn(dto);

            StepVerifier.create(controller.translateByCodeMessageAndEnvironment("CODE", body, exchange()))
                    .verifyComplete();

            verify(translate).execute(eq("CODE"), any(MessageAccessContext.class), eq("auto"), eq("en"));
            verify(translationPresenter).presentRestSuccess(List.of(dto), null);
        }

        private TranslatedMessageDTO translatedMessage(String source, String target) {
            return TranslatedMessageDTO.create("CODE", source, target, "T", "C", "TT", "TC", "TYPE",
                    "translated TYPE", "CAT", "translated CAT", "FUNC", "translated FUNC", "provider", "model", 10);
        }
    }

    @Nested
    @DisplayName("events")
    class Events {

        @Test
        @DisplayName("Emite el evento del stream como ServerSentEvent")
        void emitsStreamEventAsServerSentEvent() {
            SubmissionPublisher<MessageEvent> publisher = new SubmissionPublisher<>();
            when(eventStream.subscribe("env-1")).thenReturn(publisher);
            MessageEvent event = new MessageEvent("id-1", "MessageCreated", "env-1", "CODE");

            try {
                StepVerifier.create(controller.events(exchange()).take(1))
                        .then(() -> publisher.submit(event))
                        .assertNext(serverSentEvent -> assertAll(
                                () -> assertThat(serverSentEvent.id()).isEqualTo("id-1"),
                                () -> assertThat(serverSentEvent.event()).isEqualTo("MessageCreated"),
                                () -> assertThat(serverSentEvent.data()).isSameAs(event)))
                        .verifyComplete();
            } finally {
                publisher.close();
            }
        }

        @Test
        @DisplayName("Emite un heartbeat cuando no hay eventos del stream")
        void emitsHeartbeatWhenNoStreamEvent() {
            SubmissionPublisher<MessageEvent> publisher = new SubmissionPublisher<>();
            when(eventStream.subscribe("env-1")).thenReturn(publisher);

            try {
                StepVerifier.withVirtualTime(() -> controller.events(exchange()).take(1))
                        .thenAwait(Duration.ofSeconds(31))
                        .assertNext(serverSentEvent -> assertAll(
                                () -> assertThat(serverSentEvent.event()).isEqualTo("heartbeat"),
                                () -> assertThat(serverSentEvent.id()).isNotBlank()))
                        .verifyComplete();
            } finally {
                publisher.close();
            }
        }

        @Test
        @DisplayName("Rechaza el stream cuando no existe ambiente en el contexto")
        void rejectsMissingEnvironment() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

            StepVerifier.create(controller.events(exchange))
                    .expectError(UnauthorizedException.class)
                    .verify();

            verify(eventStream, never()).subscribe(any());
        }
    }
}
