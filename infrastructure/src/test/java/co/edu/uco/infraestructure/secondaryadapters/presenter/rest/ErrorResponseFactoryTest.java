package co.edu.uco.infraestructure.secondaryadapters.presenter.rest;

import co.edu.uco.application.secondaryports.ErrorDetail;
import co.edu.uco.application.secondaryports.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ErrorResponseFactory")
class ErrorResponseFactoryTest {

    @Nested
    @DisplayName("build")
    class Build {

        @Test
        @DisplayName("Construye la respuesta con código, mensaje y ruta de la petición")
        void build_includesCodeMessageAndRequestPath() {
            var request = MockServerHttpRequest.get("/messageucolab/v1/messages").build();

            ErrorResponse response = ErrorResponseFactory.build("NOT_FOUND", "No encontrado", request);

            assertAll(
                    () -> assertThat(response.errors()).singleElement()
                            .extracting(ErrorDetail::code, ErrorDetail::message)
                            .containsExactly("NOT_FOUND", "No encontrado"),
                    () -> assertThat(response.path()).isEqualTo("/messageucolab/v1/messages"),
                    () -> assertThat(response.timestamp()).isNotBlank());
        }

        @Test
        @DisplayName("Usa ruta vacía cuando la petición es nula")
        void build_usesEmptyPathWhenRequestIsNull() {
            ErrorResponse response = ErrorResponseFactory.build("BAD_REQUEST", "Solicitud inválida", null);

            assertThat(response.path()).isEmpty();
        }

        @Test
        @DisplayName("Usa ruta vacía cuando la petición no expone path")
        void build_usesEmptyPathWhenRequestPathIsNull() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getPath()).thenReturn(null);

            ErrorResponse response = ErrorResponseFactory.build("BAD_REQUEST", "Solicitud inválida", request);

            assertThat(response.path()).isEmpty();
        }
    }

    @Nested
    @DisplayName("codeForStatus")
    class CodeForStatus {

        @ParameterizedTest(name = "status {0} -> {1}")
        @CsvSource({
                "400, BAD_REQUEST",
                "401, UNAUTHORIZED",
                "403, FORBIDDEN",
                "404, NOT_FOUND",
                "406, NOT_ACCEPTABLE",
                "409, CONFLICT",
                "422, UNPROCESSABLE_ENTITY",
                "500, INTERNAL_SERVER_ERROR",
                "418, INTERNAL_SERVER_ERROR"
        })
        @DisplayName("Traduce el código HTTP al código semántico esperado")
        void mapsStatusToSemanticCode(int httpStatus, String expectedCode) {
            assertThat(ErrorResponseFactory.codeForStatus(httpStatus)).isEqualTo(expectedCode);
        }
    }
}
