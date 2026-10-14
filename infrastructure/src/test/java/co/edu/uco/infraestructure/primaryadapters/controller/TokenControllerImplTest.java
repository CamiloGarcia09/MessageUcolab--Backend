package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.token.CreateTokenDTO;
import co.edu.uco.application.primaryports.facade.token.CreateTokenUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenControllerImpl")
class TokenControllerImplTest {

    @Mock
    private CreateTokenUseCaseFacade facade;
    @Mock
    private PresenterPort<String> presenter;

    private TokenControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new TokenControllerImpl(facade, presenter);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post("/messageucolab/v1/tokens").build());
    }

    private void stubRender() {
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    @Nested
    @DisplayName("createToken")
    class CreateToken {

        @Test
        @DisplayName("Ejecuta el caso de uso con la aplicación y presenta el token")
        void executesAndPresentsToken() {
            stubRender();
            CreateTokenDTO dto = new CreateTokenDTO("2026-01-01", "env-1");
            when(facade.execute(dto, "app-id")).thenReturn("token-1");

            StepVerifier.create(controller.createToken(dto, "app-id", exchange())).verifyComplete();

            verify(facade).execute(dto, "app-id");
            verify(presenter).presentRestSuccess(List.of("token-1"), null);
        }

        @Test
        @DisplayName("Convierte un resultado nulo a texto antes de presentar")
        void nullResult_isPresentedAsLiteralNull() {
            stubRender();
            CreateTokenDTO dto = new CreateTokenDTO("2026-01-01", "env-1");
            when(facade.execute(dto, "app-id")).thenReturn(null);

            StepVerifier.create(controller.createToken(dto, "app-id", exchange())).verifyComplete();

            verify(presenter).presentRestSuccess(List.of("null"), null);
        }

        @Test
        @DisplayName("Propaga el error del caso de uso sin presentar")
        void propagatesFacadeFailure() {
            CreateTokenDTO dto = new CreateTokenDTO("2026-01-01", "env-1");
            ConflictException failure = ConflictException.buildUserException("token ya emitido");
            when(facade.execute(dto, "app-id")).thenThrow(failure);

            StepVerifier.create(controller.createToken(dto, "app-id", exchange()))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();

            verify(presenter, never()).presentRestSuccess(anyList(), any());
        }
    }
}
