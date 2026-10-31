package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactiveHttpResponse")
class ReactiveHttpResponseTest {

    @Mock
    private PresenterPort<String> presenter;

    @Nested
    @DisplayName("present")
    class Present {

        @Test
        @DisplayName("Escribe el cuerpo, el content-type y el estado 200 del presentador")
        void present_writesPresenterBodyAndContentType() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/catalog/applications")
                            .header("Accept", "application/json").build());
            when(presenter.presentRestSuccess(List.of("dto"), "application/json"))
                    .thenReturn(new RenderedResponse("{\"data\":[\"dto\"]}", "application/json"));

            StepVerifier.create(ReactiveHttpResponse.present(presenter, List.of("dto"), exchange))
                    .verifyComplete();

            assertAll(
                    () -> assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(exchange.getResponse().getHeaders().getContentType())
                            .isEqualTo(MediaType.APPLICATION_JSON),
                    () -> assertThat(exchange.getResponse().getBodyAsString().block())
                            .isEqualTo("{\"data\":[\"dto\"]}"));
            verify(presenter).presentRestSuccess(List.of("dto"), "application/json");
        }

        @Test
        @DisplayName("Usa accept nulo cuando la petición no declara cabecera Accept")
        void present_usesNullAcceptWhenHeaderIsAbsent() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/catalog/applications").build());
            when(presenter.presentRestSuccess(List.of("dto"), null))
                    .thenReturn(new RenderedResponse("body", "text/plain"));

            StepVerifier.create(ReactiveHttpResponse.present(presenter, List.of("dto"), exchange))
                    .verifyComplete();

            assertThat(exchange.getResponse().getHeaders().getContentType())
                    .isEqualTo(MediaType.TEXT_PLAIN);
            verify(presenter).presentRestSuccess(List.of("dto"), null);
        }

        @Test
        @DisplayName("Propaga el fallo del presentador sin escribir cuerpo")
        void present_propagatesPresenterFailure() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/catalog/applications").build());
            BusinessException failure = BusinessException.buildUserException("fallo de serialización");
            when(presenter.presentRestSuccess(List.of("dto"), null)).thenThrow(failure);

            StepVerifier.create(ReactiveHttpResponse.present(presenter, List.of("dto"), exchange))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();
        }
    }

    @Nested
    @DisplayName("blocking")
    class Blocking {

        @Test
        @DisplayName("Emite el resultado del Callable")
        void blocking_emitsCallableResult() {
            StepVerifier.create(ReactiveHttpResponse.blocking(() -> "value"))
                    .expectNext("value")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Propaga la excepción del Callable")
        void blocking_propagatesCallableFailure() {
            BusinessException failure = BusinessException.buildUserException("fallo de caso de uso");

            StepVerifier.create(ReactiveHttpResponse.blocking(() -> {
                        throw failure;
                    }))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();
        }
    }

    @Nested
    @DisplayName("attribute")
    class Attribute {

        @Test
        @DisplayName("Devuelve el atributo almacenado en el exchange")
        void attribute_returnsStoredValue() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/").build());
            exchange.getAttributes().put("environmentId", "env-1");

            assertThat(ReactiveHttpResponse.attribute(exchange, "environmentId")).isEqualTo("env-1");
        }

        @Test
        @DisplayName("Devuelve null cuando el atributo no existe")
        void attribute_returnsNullWhenAbsent() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/").build());

            assertThat(ReactiveHttpResponse.attribute(exchange, "unknown")).isNull();
        }
    }
}
