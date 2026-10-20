package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActiveContextInterceptor")
class ActiveContextInterceptorTest {

    private static final ExternalIdentity IDENTITY =
            new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);

    @Mock
    private ActiveContextUseCaseFacade activeContext;
    @Mock
    private CatalogPort catalog;
    @Mock
    private WebFilterChain chain;

    private ActiveContextInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ActiveContextInterceptor(activeContext, catalog);
    }

    @Test
    @DisplayName("No procesa rutas fuera de la API")
    void filter_outsideApi_continuesWithoutIdentity() {
        MockServerWebExchange exchange = exchange("/actuator/health");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Rechaza Token y Authorization sin identidad verificada")
    void filter_unresolvedAuthorizationAndTokenWithoutIdentity_returnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/messages")
                        .header("Token", "legacy")
                        .header("Authorization", "Bearer unresolved")
                        .build());

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

    @Test
    @DisplayName("Resuelve y guarda el ambiente externo antes de continuar")
    void filter_withIdentity_setsEnvironmentAndContinues() {
        MockServerWebExchange exchange = exchange("/messageucolab/v1/messages");
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        when(activeContext.findActiveContext(IDENTITY))
                .thenReturn(ActiveContextDTO.builder().environmentId("env-1").build());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isEqualTo("env-1");
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Permite /me con identidad sin requerir contexto de ambiente")
    void filter_mePathWithIdentity_continuesWithoutResolvingEnvironment() {
        MockServerWebExchange exchange = exchange("/messageucolab/v1/me/profile");
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verify(activeContext, never()).findActiveContext(IDENTITY);
    }

    @Test
    @DisplayName("Requiere identidad externa en /me")
    void filter_mePathWithoutIdentity_returnsUnauthorized() {
        MockServerWebExchange exchange = exchange("/messageucolab/v1/me/profile");

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

    @Test
    @DisplayName("Traduce una ausencia de contexto a 401")
    void filter_notFound_mapsToUnauthorized() {
        MockServerWebExchange missing = exchange("/messageucolab/v1/messages");
        missing.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        when(activeContext.findActiveContext(IDENTITY))
                .thenThrow(NotFoundException.buildUserException("sin contexto"));

        StepVerifier.create(interceptor.filter(missing, chain)).verifyComplete();
        assertThat(missing.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(missing);
    }

    @Test
    @DisplayName("Un contexto ausente que produce Unauthorized no permite continuar")
    void filter_missingContextMapsUnauthorizedAndStopsChain() {
        MockServerWebExchange exchange = exchange("/messageucolab/v1/messages");
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        when(activeContext.findActiveContext(IDENTITY)).thenReturn(null);
        when(catalog.getMessage("FUN_152")).thenReturn("contexto requerido");

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

    @Test
    @DisplayName("Traduce una denegación a 403")
    void filter_forbidden_mapsToForbidden() {
        MockServerWebExchange exchange = exchange("/messageucolab/v1/messages");
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        when(activeContext.findActiveContext(IDENTITY))
                .thenThrow(ForbiddenException.buildUserException("sin permiso"));

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(exchange);
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}
