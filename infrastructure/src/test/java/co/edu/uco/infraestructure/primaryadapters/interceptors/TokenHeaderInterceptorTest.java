package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.facade.token.FindEnvironmentIdTokenUseCaseFacade;
import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.token.VerifyAccessUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
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

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.REQUEST_GET_HEADER_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenHeaderInterceptor")
class TokenHeaderInterceptorTest {

    @Mock
    private VerifyAccessUseCaseFacade verifyAccess;
    @Mock
    private FindEnvironmentIdTokenUseCaseFacade findEnvironment;
    @Mock
    private ActiveContextUseCaseFacade activeContext;
    @Mock
    private CatalogPort catalog;
    @Mock
    private WebFilterChain chain;

    private TokenHeaderInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TokenHeaderInterceptor(verifyAccess, findEnvironment, activeContext, catalog);
    }

    private MockServerWebExchange exchangeWithToken(String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages").header(REQUEST_GET_HEADER_TOKEN, token).build());
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("Valida el token, publica el ambiente y continúa la cadena")
        void validToken_setsEnvironmentAndContinues() {
            MockServerWebExchange exchange = exchangeWithToken("token-abc");
            when(verifyAccess.execute("token-abc")).thenReturn(true);
            when(findEnvironment.execute("token-abc")).thenReturn("env-123");
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isEqualTo("env-123");
            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Responde 401 cuando el token no es válido")
        void invalidToken_returns401() {
            MockServerWebExchange exchange = exchangeWithToken("token-abc");
            when(verifyAccess.execute("token-abc")).thenReturn(false);
            when(catalog.getMessage("TCH_031")).thenReturn("token no autorizado");

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertAll(
                    () -> assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
                    () -> assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isNull());
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Responde 401 cuando no llega cabecera Token")
        void missingToken_returns401() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Resuelve el ambiente activo y continúa cuando ya hay identidad externa")
        void existingExternalIdentity_resolvesActiveEnvironment() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            ExternalIdentity identity = new ExternalIdentity("issuer", "subject", "user@uco.edu.co",
                    java.time.Instant.MAX);
            exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, identity);
            when(activeContext.findActiveContext(identity))
                    .thenReturn(ActiveContextDTO.builder().environmentId("env-external").build());
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isEqualTo("env-external");
            verifyNoInteractions(verifyAccess, findEnvironment);
            verify(activeContext).findActiveContext(identity);
        }

        @Test
        @DisplayName("Responde 401 cuando la identidad externa no tiene ambiente activo")
        void externalIdentityWithoutActiveEnvironment_returns401() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            ExternalIdentity identity = new ExternalIdentity("issuer", "subject", "user@uco.edu.co",
                    java.time.Instant.MAX);
            exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, identity);
            when(activeContext.findActiveContext(identity)).thenReturn(null);
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente no resuelto");

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Responde 401 cuando el ambiente del token no existe")
        void environmentNotFound_returns401() {
            MockServerWebExchange exchange = exchangeWithToken("token-abc");
            when(verifyAccess.execute("token-abc")).thenReturn(true);
            when(findEnvironment.execute("token-abc")).thenThrow(NotFoundException.buildUserException("ambiente inexistente"));

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Responde 401 cuando la validación lanza UnauthorizedException")
        void unauthorizedFromValidation_returns401() {
            MockServerWebExchange exchange = exchangeWithToken("token-abc");
            when(verifyAccess.execute("token-abc")).thenThrow(UnauthorizedException.buildUserException("no autorizado"));

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(exchange);
        }

        @Test
        @DisplayName("Responde 403 cuando la resolución de ambiente lanza ForbiddenException")
        void forbiddenFromResolution_returns403() {
            MockServerWebExchange exchange = exchangeWithToken("token-abc");
            when(verifyAccess.execute("token-abc")).thenReturn(true);
            when(findEnvironment.execute("token-abc")).thenThrow(ForbiddenException.buildUserException("prohibido"));

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(chain, never()).filter(exchange);
        }
    }
}
