package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerType;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json.JsonSerializer;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
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

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveMessageControllerImpl")
class ResolveMessageControllerImplTest {

    @Mock
    private ResolveMessageUseCaseFacade facade;
    @Mock private SerializerRegistry serializers;
    @Mock private SerializerType serializer;

    private ResolveMessageControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new ResolveMessageControllerImpl(facade, serializers);
    }

    private void stubRender() {
        when(serializers.getSerializerForMediaType("application/json")).thenReturn(serializer);
        when(serializer.serialize(any())).thenReturn("{\"code\":\"CODE\",\"title\":\"Title\",\"content\":\"Content\"}");
    }

    private MockServerWebExchange exchange(String environmentId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages/consumer").build());
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, environmentId);
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, "app-1");
        return exchange;
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("Delega con el código y presenta el mensaje resuelto")
        void delegatesAndPresentsResolvedMessage() {
            stubRender();
            var expected = new ResolvedMessageDTO("CODE", "Title", "Content");
            when(facade.execute(eq("CODE"), eq("env-1"), any(MessageAccessContext.class))).thenReturn(expected);

            var exchange = exchange("env-1");
            StepVerifier.create(controller.resolve("CODE", exchange)).verifyComplete();

            verify(facade).execute(eq("CODE"), eq("env-1"), any(MessageAccessContext.class));
            verify(serializer).serialize(expected);
            assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        }

        @Test
        @DisplayName("Construye el contexto con la aplicación validada de la sesión")
        void buildsContextWithSessionApplication() {
            stubRender();
            MockServerWebExchange exchange = exchange("env-1");
            var expected = new ResolvedMessageDTO("CODE", "Title", "Content");
            when(facade.execute(eq("CODE"), eq("env-1"), any(MessageAccessContext.class))).thenReturn(expected);

            StepVerifier.create(controller.resolve("CODE", exchange)).verifyComplete();

            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(facade).execute(eq("CODE"), eq("env-1"), contextCaptor.capture());
            assertAll(
                    () -> assertThat(contextCaptor.getValue().applicationId()).isEqualTo("app-1"));
        }

        @Test
        @DisplayName("Escribe el JSON del consumidor sin envelope y con no-store")
        void writesExactConsumerJsonAndNoStoreHeader() {
            var actualSerializer = new JsonSerializer();
            when(serializers.getSerializerForMediaType("application/json")).thenReturn(actualSerializer);
            var expected = new ResolvedMessageDTO("CODE", "Title", "Body", "TYPE", "CATEGORY", "FUNC",
                    java.time.Instant.parse("2025-01-02T03:04:05Z"),
                    java.time.Instant.parse("2025-02-03T04:05:06Z"));
            when(facade.execute(eq("CODE"), eq("env-1"), any(MessageAccessContext.class))).thenReturn(expected);
            var exchange = exchange("env-1");

            StepVerifier.create(controller.resolve("CODE", exchange)).verifyComplete();

            assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(
                    "{\"code\":\"CODE\",\"title\":\"Title\",\"content\":\"Body\",\"type\":\"TYPE\","
                            + "\"category\":\"CATEGORY\",\"functionality\":\"FUNC\","
                            + "\"created_at\":\"2025-01-02T03:04:05Z\",\"updated_at\":\"2025-02-03T04:05:06Z\"}");
            assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        }

        @Test
        @DisplayName("Propaga el error del caso de uso sin presentar")
        void propagatesFacadeFailure() {
            NotFoundException failure = NotFoundException.buildUserException("mensaje inexistente");
            when(facade.execute(eq("CODE"), eq("env-1"), any(MessageAccessContext.class))).thenThrow(failure);

            StepVerifier.create(controller.resolve("CODE", exchange("env-1")))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();

            verify(serializer, never()).serialize(any());
        }
    }
}
