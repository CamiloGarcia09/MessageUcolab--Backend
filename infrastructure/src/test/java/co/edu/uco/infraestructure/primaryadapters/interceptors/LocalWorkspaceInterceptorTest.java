package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.infraestructure.config.LocalWorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalWorkspaceInterceptor")
class LocalWorkspaceInterceptorTest {

    @Mock
    private WebFilterChain chain;

    private LocalWorkspaceProperties properties;
    private LocalWorkspaceInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new LocalWorkspaceProperties(
                UUID.fromString("8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40"),
                "UCO",
                UUID.fromString("c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23"),
                "Arquisoft",
                UUID.fromString("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"),
                "Develop");
        interceptor = new LocalWorkspaceInterceptor(properties);
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("Permite /messageucolab/v1/workspace/local y fija ENVIRONMENT_ID_ATTRIBUTE")
        void allowsLocalWorkspacePath_setsEnvironmentAndContinues() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/workspace/local").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Elimina EXTERNAL_IDENTITY_ATTRIBUTE si existe")
        void removesExternalIdentityAttribute() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/workspace/local").build());
            exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, new Object());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((Object) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isNull();
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Permite /messageucolab/v1/messages exacto")
        void allowsMessagesExactPath() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Permite subrutas de /messageucolab/v1/messages/")
        void allowsMessagesSubroutes() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages/resolve").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Permite /messageucolab/v1/catalog/ subrutas")
        void allowsCatalogSubroutes() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/catalog/application").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Permite /messageucolab/v1/application exacto")
        void allowsApplicationExact() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/application").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Permite /messageucolab/v1/application/functionality exacto")
        void allowsApplicationFunctionalityExact() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/application/functionality").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                    .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Retorna 404 para /messageucolab/v1/me y no continua la cadena")
        void deniesMe_returns404() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/me/context").build());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertAll(
                    () -> assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isNull());
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Retorna 404 para /messageucolab/v1/token/emit y no continua la cadena")
        void deniesTokenEmit_returns404() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/messageucolab/v1/token/emit").build());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Para rutas fuera de /messageucolab/v1/ delega a la cadena sin modificar atributos")
        void outsidePrefix_delegatesToChain() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/actuator/health").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isNull();
            assertThat((Object) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isNull();
            verify(chain).filter(exchange);
        }
    }
}
