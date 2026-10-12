package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.secondaryports.security.ExternalIdentityResolverPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
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

import java.time.Instant;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalIdentityInterceptor")
class ExternalIdentityInterceptorTest {

    @Mock
    private ExternalIdentityResolverPort resolver;
    @Mock
    private WebFilterChain chain;

    private ExternalIdentityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ExternalIdentityInterceptor(resolver);
    }

    private MockServerWebExchange exchangeWithAuthorization(String value) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages").header("Authorization", value).build());
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("Continúa la cadena cuando no hay cabecera Authorization")
        void absentAuthorization_continuesChain() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Continúa la cadena cuando la cabecera Authorization está en blanco")
        void blankAuthorization_continuesChain() {
            MockServerWebExchange exchange = exchangeWithAuthorization("   ");
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Resuelve la identidad, la publica en el exchange y continúa la cadena")
        void validBearer_resolvesIdentityAndContinues() {
            MockServerWebExchange exchange = exchangeWithAuthorization("Bearer token-abc");
            ExternalIdentity identity = new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);
            when(resolver.resolve("token-abc")).thenReturn(identity);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isSameAs(identity);
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Acepta el esquema Bearer sin distinguir mayúsculas")
        void lowercaseBearer_resolvesIdentity() {
            MockServerWebExchange exchange = exchangeWithAuthorization("bearer token-abc");
            ExternalIdentity identity = new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);
            when(resolver.resolve("token-abc")).thenReturn(identity);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isSameAs(identity);
        }

        @Test
        @DisplayName("Responde 401 y no continúa cuando el esquema Bearer es inválido")
        void malformedAuthorization_returns401() {
            MockServerWebExchange exchange = exchangeWithAuthorization("Token sinBearer");

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Responde 401 cuando el resolvedor lanza UnauthorizedException")
        void resolverFailure_returns401() {
            MockServerWebExchange exchange = exchangeWithAuthorization("Bearer token-abc");
            when(resolver.resolve("token-abc")).thenThrow(UnauthorizedException.buildUserException("token inválido"));

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertAll(
                    () -> assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
                    () -> assertThat((ExternalIdentity) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isNull());
            verify(chain, never()).filter(exchange);
        }
    }
}
