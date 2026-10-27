package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslateMessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.primaryports.facade.catalog.impl.FindCatalogUseCaseFacadeImpl;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.MessageEventStreamPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.usecase.FindCatalogUseCase;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.SubmissionPublisher;
import java.time.Duration;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagesControllerImpl")
class MessagesControllerImplTest {


    @Mock
    private FindMessagesByApplicationUseCaseFacade findPage;
    @Mock
    private FindMessageByCodeAndApplicationUseCaseFacade findOne;
    @Mock
    private TranslateMessageByCodeAndApplicationUseCaseFacade translate;
    @Mock
    private PresenterPort<MessageDTO> presenter;
    @Mock
    private PresenterPort<TranslatedMessageDTO> translationPresenter;
    @Mock
    private PresenterPort<SimplePage<MessageDTO>> pagePresenter;
    @Mock
    private MessageEventStreamPort eventStream;
    @Mock
    private FindCatalogUseCaseFacade catalogFacade;
    @Mock
    private CatalogPort catalog;
    @Mock private ApplicationCatalogRepository applicationCatalogRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private EnvironmentCatalogRepository environmentCatalogRepository;
    @Mock private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock private MessageTypeCatalogRepository messageTypeCatalogRepository;
    @Mock private MessageCategoryCatalogRepository messageCategoryCatalogRepository;
    @Mock private MessageStateCatalogRepository messageStateCatalogRepository;
    @Mock private MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository;

    private MessagesControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new MessagesControllerImpl(findPage, findOne, translate, presenter, translationPresenter,
                pagePresenter, eventStream, catalogFacade, catalog);
    }

    private MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages").build());
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, "env-1");
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, "app-1");
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
    @DisplayName("findMessagesByApplication")
    class FindMessagesByApplication {

        @Test
        @DisplayName("Construye el PageRequest y presenta la página con el ambiente del token")
        void buildsPageRequestAndPresents() {
            stubPageRender();
            SimplePage<MessageDTO> page = SimplePage.of(List.of(), 1, 10, 0, 0);
            when(findPage.execute(any(MessageAccessContext.class), any(PageRequestDTO.class))).thenReturn(page);

            StepVerifier.create(controller.findMessagesByApplication(
                            "1", "10", "asc", "code",
                            null, null, null, null, null, null, "env-a,env-b",
                            "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82",
                            "c9dc13ee-2f0e-4912-946d-e7e4352c6510",
                            exchange()))
                    .verifyComplete();

            ArgumentCaptor<PageRequestDTO> requestCaptor = ArgumentCaptor.forClass(PageRequestDTO.class);
            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(findPage).execute(contextCaptor.capture(), requestCaptor.capture());
            PageRequestDTO captured = requestCaptor.getValue();
            assertAll(
                    () -> assertThat(contextCaptor.getValue().applicationId()).isEqualTo("app-1"),
                    () -> assertThat(captured.getPage()).isEqualTo("1"),
                    () -> assertThat(captured.getSize()).isEqualTo("10"),
                    () -> assertThat(captured.getSort()).isEqualTo("asc"),
                    () -> assertThat(captured.getColumnSort()).isEqualTo("code"),
                    () -> assertThat(captured.getFilterEnvironments()).isEqualTo("env-a,env-b"),
                    () -> assertThat(captured.getFilterStatusId())
                            .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"),
                    () -> assertThat(captured.getFilterFunctionalityId())
                            .isEqualTo("c9dc13ee-2f0e-4912-946d-e7e4352c6510"));
            verify(pagePresenter).presentRestSuccess(List.of(page), null);
        }
    }

    @Nested
    @DisplayName("findMessageByCodeAndApplication")
    class FindMessageByCodeAndApplication {

        @Test
        @DisplayName("Delega con el código y presenta el mensaje encontrado")
        void delegatesAndPresents() {
            stubMessageRender();
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "TYPE", "CAT", "FUNC");
            when(findOne.execute(eq("CODE"), any(MessageAccessContext.class))).thenReturn(dto);

            StepVerifier.create(controller.findMessageByCodeAndApplication("CODE", exchange())).verifyComplete();

            verify(findOne).execute(eq("CODE"), any(MessageAccessContext.class));
            verify(presenter).presentRestSuccess(List.of(dto), null);
        }
    }

    @Nested
    @DisplayName("translateMessageByCodeAndApplication")
    class TranslateMessageByCodeAndApplication {

        @Test
        @DisplayName("Delega con los idiomas origen y destino y presenta la traducción")
        void delegatesWithSourceAndTargetLanguages() {
            stubTranslationRender();
            TranslatedMessageDTO dto = translatedMessage("es", "en");
            TranslateMessageDTO body = new TranslateMessageDTO();
            body.setSourceLanguage("es");
            body.setTargetLanguage("en");
            when(translate.execute(eq("CODE"), any(MessageAccessContext.class), eq("es"), eq("en"))).thenReturn(dto);

            StepVerifier.create(controller.translateMessageByCodeAndApplication("CODE", body, exchange()))
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

            StepVerifier.create(controller.translateMessageByCodeAndApplication("CODE", body, exchange()))
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
            when(eventStream.subscribe(Set.of())).thenReturn(publisher);
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
            when(eventStream.subscribe(Set.of())).thenReturn(publisher);

            try {
                StepVerifier.withVirtualTime(() -> controller.events(exchange()).take(1))
                        .thenAwait(Duration.ofSeconds(31))
                        .assertNext(serverSentEvent -> assertAll(
                                () -> assertThat(serverSentEvent.event()).isEqualTo("heartbeat"),
                                () -> assertThat(serverSentEvent.data().environmentId()).isEmpty(),
                                () -> assertThat(serverSentEvent.data().messageCode()).isEmpty(),
                                () -> assertThat(serverSentEvent.data().id()).isNotBlank()))
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

            verify(eventStream, never()).subscribe(anyString());
            verify(eventStream, never()).subscribe(anySet());
        }

        @Test
        @DisplayName("En modo sesion se suscribe a todos los ambientes de la aplicacion")
        void subscribesToEveryEnvironmentOfTheSessionApplication() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages/events").build());
            exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, "app-1");
            List<CatalogItemDTO> sessionApplications = List.of(CatalogItemDTO.create("app-1", "app"));
            exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, sessionApplications);
            when(catalogFacade.findEnvironmentsByApplication("app-1", sessionApplications)).thenReturn(List.of(
                    CatalogItemDTO.create("env-dev", "Develop"), CatalogItemDTO.create("env-prod", "Production")));
            SubmissionPublisher<MessageEvent> publisher = new SubmissionPublisher<>();
            when(eventStream.subscribe(Set.of("env-dev", "env-prod"))).thenReturn(publisher);
            MessageEvent event = new MessageEvent("id-2", "MessageCreated", "env-prod", "CODE");

            try {
                StepVerifier.create(controller.events(exchange).take(1))
                        .then(() -> publisher.submit(event))
                        .assertNext(serverSentEvent -> assertThat(serverSentEvent.data()).isSameAs(event))
                        .verifyComplete();
            } finally {
                publisher.close();
            }
        }

        @Test
        @DisplayName("Deniega la aplicación si faltan las aplicaciones de sesión antes de suscribirse")
        void rejectsApplicationWhenSessionApplicationsAreMissing() {
            assertSessionAccessDenied(null);
        }

        @Test
        @DisplayName("Deniega la aplicación ajena a sesión antes de suscribirse")
        void rejectsApplicationWhenNotAssignedToSession() {
            assertSessionAccessDenied(List.of(CatalogItemDTO.create(
                    "72436a46-54d7-4cb5-a42c-af14f92d9416", "Other app")));
        }

        private void assertSessionAccessDenied(List<CatalogItemDTO> sessionApplications) {
            String applicationId = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages/events").build());
            exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, applicationId);
            if (sessionApplications != null) {
                exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, sessionApplications);
            }
            when(catalog.getMessage("FUN_153")).thenReturn("Application is not assigned");
            FindCatalogUseCase realUseCase = new FindCatalogUseCase(applicationCatalogRepository,
                    environmentCatalogRepository, functionalityCatalogRepository, messageTypeCatalogRepository,
                    messageCategoryCatalogRepository, messageStateCatalogRepository,
                    messageEnvironmentStateCatalogRepository,
                    new SessionApplicationAccessCompositeValidator(catalog, applicationRepository));
            MessagesControllerImpl securedController = new MessagesControllerImpl(findPage, findOne, translate,
                    presenter, translationPresenter, pagePresenter, eventStream,
                    new FindCatalogUseCaseFacadeImpl(realUseCase), catalog);

            assertThatThrownBy(() -> securedController.events(exchange))
                    .isInstanceOf(co.edu.uco.crosscutting.exceptions.ForbiddenException.class);

            verifyNoInteractions(environmentCatalogRepository, eventStream);
            verify(applicationRepository, never()).findById(applicationId);
        }
    }
}
