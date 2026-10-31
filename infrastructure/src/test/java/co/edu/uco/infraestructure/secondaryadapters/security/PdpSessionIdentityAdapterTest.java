package co.edu.uco.infraestructure.secondaryadapters.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.SessionIdentity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient.Redirect;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_152;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_153;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_076;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdpSessionIdentityAdapter")
class PdpSessionIdentityAdapterTest {
    @Mock private HttpClient httpClient;
    @Mock private CatalogPort catalog;
    @Mock private HttpResponse<String> response;

    private PdpSessionIdentityAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PdpSessionIdentityAdapter("http://pdp.test///", "trusted-source",
                new ObjectMapper(), catalog, httpClient);
    }

    @Test
    @DisplayName("sends session cookie and maps the BFF identity contract")
    void resolve_validResponse_sendsCookieAndBuildsTrustedIdentity() throws IOException, InterruptedException {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":{\"subject\":\"sub-1\",\"tenantId\":\"tenant-1\","
                + "\"email\":\"user@example.test\",\"name\":\"User\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        SessionIdentity identity = adapter.resolve("session-token");

        assertThat(identity).isEqualTo(new SessionIdentity("trusted-source", "tenant-1", "sub-1",
                "user@example.test", "User"));
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(captor.getValue().uri().toString()).isEqualTo("http://pdp.test/api/v1/session");
        assertThat(captor.getValue().headers().firstValue("Cookie")).hasValue("SECURITY_BASELINE_SESSION=session-token");
        assertThat(captor.getValue().headers().firstValue("Accept")).hasValue("application/json");
    }

    @Test
    @DisplayName("rejects missing session without contacting the identity provider")
    void resolve_blankSession_throwsUnauthorizedWithoutHttpRequest() throws IOException, InterruptedException {
        when(catalog.getMessage(FUN_152.getCode())).thenReturn("Authentication required");

        assertThatThrownBy(() -> adapter.resolve("  ")).isInstanceOf(UnauthorizedException.class);

        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("maps unauthorized and forbidden status codes to domain exceptions")
    void resolve_authFailureStatus_preservesDomainMeaning() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(catalog.getMessage(FUN_152.getCode())).thenReturn("Authentication required");
        when(catalog.getMessage(FUN_153.getCode())).thenReturn("Access denied");
        when(response.statusCode()).thenReturn(401, 403);

        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("fails closed on malformed or incomplete identity bodies")
    void resolve_malformedOrIncompleteBody_throwsTechnicalBusinessException() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{invalid", "{\"data\":{\"subject\":\" \"}}");
        when(catalog.getMessage(TCH_076.getCode())).thenReturn("Identity service unavailable");

        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("does not follow redirects or accept other successful-looking statuses")
    void resolve_redirectOrUnexpectedStatus_throwsTechnicalBusinessExceptionAfterSingleResponse()
            throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(302, 204);
        when(catalog.getMessage(TCH_076.getCode())).thenReturn("Identity service unavailable");

        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(BusinessException.class);

        verify(httpClient, times(2))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("translates request timeouts into technical domain failures")
    void resolve_timeout_throwsTechnicalBusinessException() throws IOException, InterruptedException {
        when(catalog.getMessage(TCH_076.getCode())).thenReturn("Identity service unavailable");
        doThrow(new HttpTimeoutException("timeout"))
                .when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        assertThatThrownBy(() -> adapter.resolve("session")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("configures the real HTTP client not to follow redirects")
    void newHttpClient_redirectPolicyIsNever() {
        assertThat(PdpSessionIdentityAdapter.newHttpClient().followRedirects()).isEqualTo(Redirect.NEVER);
    }
}
