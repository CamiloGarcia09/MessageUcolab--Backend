package co.edu.uco.infraestructure.secondaryadapters.presenter.rest;

import co.edu.uco.application.secondaryports.ErrorDetail;
import co.edu.uco.application.secondaryports.ErrorResponse;
import co.edu.uco.application.secondaryports.Response;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpPresenterAdapter")
class HttpPresenterAdapterTest {

    @Mock
    private SerializerRegistry registry;
    @Mock
    private CatalogPort catalog;
    @Mock
    private LoggingPortFactory factory;
    @Mock
    private LoggingPort log;
    @Mock
    private SerializerType serializer;

    private HttpPresenterAdapter<String> adapter;

    @BeforeEach
    void setUp() {
        when(factory.getLogger(HttpPresenterAdapter.class)).thenReturn(log);
        adapter = new HttpPresenterAdapter<>(registry, catalog, factory);
    }

    private MockServerWebExchange exchange(String acceptHeader) {
        var request = MockServerHttpRequest.get("/messageucolab/v1/messages");
        if (acceptHeader != null) {
            request = request.header("Accept", acceptHeader);
        }
        return MockServerWebExchange.from(request.build());
    }

    @Nested
    @DisplayName("presentRestSuccess")
    class PresentRestSuccess {

        @Test
        @DisplayName("Serializa el Response con los datos y expone el content-type del serializador")
        void success_rendersBodyAndContentType() {
            when(registry.getSerializerForMediaType("application/json")).thenReturn(serializer);
            when(serializer.serialize(any(Response.class))).thenReturn("{\"data\":[\"dto\"]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");

            RenderedResponse result = adapter.presentRestSuccess(List.of("dto"), "application/json");

            ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
            verify(serializer).serialize(captor.capture());
            Response<?> serialized = captor.getValue();
            assertAll(
                    () -> assertThat(result.body()).isEqualTo("{\"data\":[\"dto\"]}"),
                    () -> assertThat(result.contentType()).isEqualTo("application/json"),
                    () -> assertThat(serialized.data()).singleElement().isEqualTo("dto"),
                    () -> assertThat(serialized.errors()).isEmpty());
        }

        @Test
        @DisplayName("Registra y repropaga el CrossWordsException de serialización")
        void serializationFailure_logsAndRethrows() {
            CrossWordsException failure = CrossWordsException.buildInfrastructure(
                    "fallo técnico", "fallo serializando", ExceptionType.TECHNICAL);
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(Response.class))).thenThrow(failure);
            when(catalog.getMessage("TCH_016")).thenReturn("error al serializar");

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> adapter.presentRestSuccess(List.of("dto"), null))
                    .isSameAs(failure);

            verify(log).error("error al serializar", failure);
        }
    }

    @Nested
    @DisplayName("presentCrossWordsException")
    class PresentCrossWordsException {

        @Test
        @DisplayName("Traduce un error técnico a 500 registrando el error")
        void technicalException_returns500AndLogsError() {
            MockServerWebExchange exchange = exchange("application/json");
            CrossWordsException failure = CrossWordsException.buildInfrastructure(
                    "fallo técnico", "mensaje de usuario", ExceptionType.TECHNICAL);
            when(registry.getSerializerForMediaType("application/json")).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");
            when(catalog.getMessage("TCH_016")).thenReturn("error interno");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .assertNext(response -> assertAll(
                            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                            () -> assertThat(response.getBody()).isEqualTo("{\"errors\":[]}"),
                            () -> assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON)))
                    .verifyComplete();

            verify(log).error("error interno", failure);
        }

        @Test
        @DisplayName("Traduce una regla de negocio a su status sin registrar error")
        void businessException_usesUserMessageWithoutErrorLog() {
            MockServerWebExchange exchange = exchange(null);
            CrossWordsException failure = BusinessException.buildUserException("regla de negocio incumplida");
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                    .verifyComplete();

            verifyNoInteractions(log);
        }

        @Test
        @DisplayName("Traduce un acceso prohibido a 403")
        void forbiddenException_returns403() {
            MockServerWebExchange exchange = exchange(null);
            CrossWordsException failure = ForbiddenException.buildUserException("acceso denegado");
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Usa el mensaje del catálogo cuando la excepción no trae mensaje de usuario")
        void exceptionWithoutUserMessage_usesCatalogFallback() {
            MockServerWebExchange exchange = exchange(null);
            CrossWordsException failure = CrossWordsException.buildInfrastructure(
                    "fallo técnico", null, ExceptionType.TECHNICAL);
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");
            when(catalog.getMessage("TCH_016")).thenReturn("error interno");
            when(catalog.getMessage("FUN_023")).thenReturn("error general");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .assertNext(response -> assertThat(response.getBody()).isEqualTo("{\"errors\":[]}"))
                    .verifyComplete();

            ArgumentCaptor<ErrorResponse> captor = ArgumentCaptor.forClass(ErrorResponse.class);
            verify(serializer).serialize(captor.capture());
            assertThat(captor.getValue().errors()).singleElement()
                    .extracting(ErrorDetail::message)
                    .isEqualTo("error general");
        }

        @Test
        @DisplayName("Asume 500 cuando la excepción de dominio no define status HTTP")
        void exceptionWithoutHttpStatus_defaultsTo500AndLogsError() {
            MockServerWebExchange exchange = exchange(null);
            CrossWordsException failure = CrossWordsException.buildInfrastructure(
                    "fallo técnico", "mensaje de usuario", ExceptionType.TECHNICAL);
            failure.setHttpStatus(0);
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");
            when(catalog.getMessage("TCH_016")).thenReturn("error interno");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR))
                    .verifyComplete();

            verify(log).error("error interno", failure);
        }

        @Test
        @DisplayName("Devuelve error reactivo cuando falla la serialización de la respuesta de error")
        void renderingFailure_returnsReactiveError() {
            MockServerWebExchange exchange = exchange(null);
            CrossWordsException failure = BusinessException.buildUserException("regla de negocio");
            CrossWordsException renderingFailure = CrossWordsException.buildInfrastructure(
                    "fallo serializando error", "no se pudo serializar", ExceptionType.TECHNICAL);
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenThrow(renderingFailure);
            when(catalog.getMessage("TCH_019")).thenReturn("error renderizando");

            StepVerifier.create(adapter.presentCrossWordsException(failure, exchange))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(renderingFailure))
                    .verify();

            verify(log).error("error renderizando", renderingFailure);
        }
    }

    @Nested
    @DisplayName("handleGeneralException")
    class HandleGeneralException {

        @Test
        @DisplayName("Traduce una excepción genérica a 500 con el mensaje del catálogo")
        void generalException_returns500WithCatalogMessage() {
            MockServerWebExchange exchange = exchange(null);
            RuntimeException failure = new RuntimeException("boom");
            when(registry.getSerializerForMediaType(null)).thenReturn(serializer);
            when(serializer.serialize(any(ErrorResponse.class))).thenReturn("{\"errors\":[]}");
            when(serializer.getSupportedContentType()).thenReturn("application/json");
            when(catalog.getMessage("FUN_023")).thenReturn("error general");
            when(catalog.getMessage("TCH_016")).thenReturn("error interno");

            StepVerifier.create(adapter.handleGeneralException(failure, exchange))
                    .assertNext(response -> assertAll(
                            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                            () -> assertThat(response.getBody()).isEqualTo("{\"errors\":[]}")))
                    .verifyComplete();

            verify(log).error("error interno", failure);
        }
    }
}
