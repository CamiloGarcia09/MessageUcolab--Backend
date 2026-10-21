package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationInterceptorTest {

    private static final String SESSION = "abc123";
    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final List<CatalogItemDTO> APPLICATIONS = List.of(CatalogItemDTO.create(APPLICATION_ID, "MessageUcoLab"));

    @Mock
    private SessionApplicationUseCaseFacade facade;

    private SessionApplicationInterceptor interceptor;
    private AtomicBoolean chainInvoked;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        interceptor = new SessionApplicationInterceptor(facade);
        chainInvoked = new AtomicBoolean();
        chain = exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
    }

    @Test
    void filter_ignoresPathsOutsideApi() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/actuator/health"));

        interceptor.filter(exchange, chain).block();

        assertThat(chainInvoked).isTrue();
        verifyNoInteractions(facade);
    }

    @Test
    void filter_resolvesSelectedApplicationWithoutEnvironment_forMessageOperations() {
        when(facade.findCurrentUserApplications(SESSION)).thenReturn(APPLICATIONS);
        when(facade.resolveApplication(APPLICATIONS, APPLICATION_ID)).thenReturn(APPLICATION_ID);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/messageucolab/v1/messages?page=1&size=20")
                .cookie(new HttpCookie("SECURITY_BASELINE_SESSION", SESSION))
                .header("X-Application-Id", APPLICATION_ID));

        interceptor.filter(exchange, chain).block();

        assertThat(chainInvoked).isTrue();
        assertThat((String) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE)).isEqualTo(APPLICATION_ID);
        assertThat((Object) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isNull();
        assertThat((Object) exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE)).isEqualTo(APPLICATIONS);
        verify(facade, never()).resolveEnvironment(any(), any(), any());
    }

    @Test
    void filter_alsoValidatesExplicitEnvironment_whenHeaderIsPresent() {
        when(facade.findCurrentUserApplications(SESSION)).thenReturn(APPLICATIONS);
        when(facade.resolveApplication(APPLICATIONS, APPLICATION_ID)).thenReturn(APPLICATION_ID);
        when(facade.resolveEnvironment(APPLICATIONS, APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(ENVIRONMENT_ID);
        MockServerWebExchange exchange = exchange(withSessionAndContext(
                MockServerHttpRequest.get("/messageucolab/v1/messages/resolve")));

        interceptor.filter(exchange, chain).block();

        assertThat((String) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE)).isEqualTo(APPLICATION_ID);
        assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isEqualTo(ENVIRONMENT_ID);
    }

    @ParameterizedTest(name = "path={0}")
    @ValueSource(strings = {
            "/messageucolab/v1/session/applications",
            "/messageucolab/v1/catalog/message-types",
            "/messageucolab/v1/application"
    })
    void filter_storesSessionApplicationsWithoutResolvingEnvironment_forNonMessagePaths(String path) {
        when(facade.findCurrentUserApplications(SESSION)).thenReturn(APPLICATIONS);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get(path).cookie(new HttpCookie(
                "SECURITY_BASELINE_SESSION", SESSION)));

        interceptor.filter(exchange, chain).block();

        assertThat(chainInvoked).isTrue();
        assertThat((Object) exchange.getAttribute(SESSION_APPLICATIONS_ATTRIBUTE)).isEqualTo(APPLICATIONS);
        assertThat((Object) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).isNull();
        assertThat((Object) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE)).isNull();
        verify(facade, never()).resolveApplication(any(), any());
    }

    @Test
    void filter_neverTrustsAnExternalIdentityAttribute() {
        when(facade.findCurrentUserApplications(SESSION)).thenReturn(APPLICATIONS);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/messageucolab/v1/catalog/applications")
                .cookie(new HttpCookie("SECURITY_BASELINE_SESSION", SESSION)));
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, new Object());

        interceptor.filter(exchange, chain).block();

        assertThat((Object) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isNull();
    }

    @Test
    void filter_passesNullSession_whenCookieIsMissing() {
        when(facade.findCurrentUserApplications(null))
                .thenThrow(UnauthorizedException.buildUserException("Authentication required"));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/messageucolab/v1/messages"));

        interceptor.filter(exchange, chain).block();

        assertRejected(exchange, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_rejectsWithForbidden_whenApplicationIsNotAssignedToSession() {
        assertResolutionErrorMapsTo(() -> ForbiddenException.buildUserException("Permission denied"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void filter_rejectsWithNotFound_whenApplicationIsNotRegistered() {
        assertResolutionErrorMapsTo(() -> NotFoundException.buildUserException("Application not found"),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void filter_rejectsWithConflict_whenEnvironmentBelongsToAnotherApplication() {
        assertResolutionErrorMapsTo(() -> ConflictException.buildUserException("Conflict"), HttpStatus.CONFLICT);
    }

    @Test
    void filter_rejectsWithUnprocessableEntity_whenContextHeadersAreInvalid() {
        assertResolutionErrorMapsTo(() -> BusinessRuleException.buildUserException("Identifiers required"),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void filter_rejectsWithServiceUnavailable_whenSecurityComponentFails() {
        when(facade.findCurrentUserApplications(SESSION))
                .thenThrow(BusinessException.buildTechnicalException("PDP unavailable"));
        MockServerWebExchange exchange = exchange(withSessionAndContext(
                MockServerHttpRequest.get("/messageucolab/v1/messages")));

        interceptor.filter(exchange, chain).block();

        assertRejected(exchange, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void assertResolutionErrorMapsTo(Supplier<RuntimeException> error, HttpStatus status) {
        when(facade.findCurrentUserApplications(SESSION)).thenReturn(APPLICATIONS);
        when(facade.resolveApplication(APPLICATIONS, APPLICATION_ID)).thenThrow(error.get());
        MockServerWebExchange exchange = exchange(withSessionAndContext(
                MockServerHttpRequest.get("/messageucolab/v1/messages")));

        interceptor.filter(exchange, chain).block();

        assertRejected(exchange, status);
        assertThat((Object) exchange.getAttribute(APPLICATION_ID_ATTRIBUTE)).isNull();
    }

    private void assertRejected(MockServerWebExchange exchange, HttpStatus status) {
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
        assertThat(chainInvoked).isFalse();
    }

    private static MockServerHttpRequest.BaseBuilder<?> withSessionAndContext(MockServerHttpRequest.BaseBuilder<?> request) {
        return request.cookie(new HttpCookie("SECURITY_BASELINE_SESSION", SESSION))
                .header("X-Application-Id", APPLICATION_ID)
                .header("X-Environment-Id", ENVIRONMENT_ID);
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request.build());
    }
}
