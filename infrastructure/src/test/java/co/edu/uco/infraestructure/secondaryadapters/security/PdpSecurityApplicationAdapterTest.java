package co.edu.uco.infraestructure.secondaryadapters.security;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdpSecurityApplicationAdapterTest {

    private static final String PDP_URL = "http://pdp.local:18080/";
    private static final String SESSION = "abc123";
    private static final Instant NOW = Instant.parse("2026-10-21T18:00:00Z");
    private static final String BODY = """
            {
              "code": "CURRENT_USER_APPLICATIONS_READ",
              "data": {
                "tenant": { "name": "Universidad UCO" },
                "user": { "name": "Laura", "email": "m@gmail.com" },
                "applications": [
                  { "id": "72436a46-54d7-4cb5-a42c-af14f92d9416", "name": "carga masiva",
                    "tenantRoles": [], "applicationRoles": [{ "name": "ADMIN" }], "profiles": [] },
                  { "id": "675f3ce2-12d0-4759-a28b-7308d5117565", "name": "prueba carga masiva" }
                ]
              }
            }
            """;

    @Mock
    private HttpClient httpClient;
    @Mock
    private CatalogPort catalogPort;

    private PdpSecurityApplicationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = adapterAt(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void findCurrentUserApplications_returnsIdAndNameOfEachApplication() throws Exception {
        respond(200, BODY);

        List<CatalogItemDTO> applications = adapter.findCurrentUserApplications(SESSION);

        assertThat(applications).containsExactly(
                CatalogItemDTO.create("72436a46-54d7-4cb5-a42c-af14f92d9416", "carga masiva"),
                CatalogItemDTO.create("675f3ce2-12d0-4759-a28b-7308d5117565", "prueba carga masiva"));
    }

    @Test
    void findCurrentUserApplications_forwardsSessionCookieToMeApplications() throws Exception {
        respond(200, BODY);
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        adapter.findCurrentUserApplications(SESSION);

        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.uri()).isEqualTo(URI.create("http://pdp.local:18080/api/v1/me/applications"));
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.headers().firstValue("Cookie")).contains("SECURITY_BASELINE_SESSION=" + SESSION);
        assertThat(request.headers().firstValue("Accept")).contains("application/json");
    }

    @Test
    void findCurrentUserApplications_returnsEmpty_whenUserHasNoApplications() throws Exception {
        respond(200, "{\"data\":{\"applications\":[]}}");

        assertThat(adapter.findCurrentUserApplications(SESSION)).isEmpty();
    }

    @Test
    void findCurrentUserApplications_reusesCachedResponseForSameSessionWithinTtl() throws Exception {
        respond(200, BODY);

        adapter.findCurrentUserApplications(SESSION);
        adapter.findCurrentUserApplications(SESSION);

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void findCurrentUserApplications_queriesAgainAfterTtlExpires() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        adapter = adapterAt(clock);
        respond(200, BODY);

        adapter.findCurrentUserApplications(SESSION);
        clock.advance(Duration.ofSeconds(31));
        adapter.findCurrentUserApplications(SESSION);

        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void findCurrentUserApplications_doesNotShareCacheBetweenSessions() throws Exception {
        respond(200, BODY);

        adapter.findCurrentUserApplications(SESSION);
        adapter.findCurrentUserApplications("other-session");

        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void findCurrentUserApplications_throwsUnauthorizedUsingFun152_whenPdpRespondsUnauthorized() throws Exception {
        respond(401, "");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode())).thenReturn("Authentication required");

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(exception -> assertThat(((UnauthorizedException) exception).getUserMessage())
                        .isEqualTo("Authentication required"));
    }

    @Test
    void findCurrentUserApplications_throwsForbiddenUsingFun153_whenPdpRespondsForbidden() throws Exception {
        respond(403, "");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn("Permission denied");

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findCurrentUserApplications_doesNotCacheRejectedSessions() throws Exception {
        respond(401, "");
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode())).thenReturn("Authentication required");

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION)).isInstanceOf(UnauthorizedException.class);

        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void findCurrentUserApplications_throwsTechnicalException_whenPdpFails() throws Exception {
        respond(503, "");

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findCurrentUserApplications_throwsTechnicalException_whenBodyIsNotJson() throws Exception {
        respond(200, "<html>login</html>");

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findCurrentUserApplications_throwsTechnicalException_whenPdpIsUnreachable() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> adapter.findCurrentUserApplications(SESSION))
                .isInstanceOf(BusinessException.class);
    }

    private PdpSecurityApplicationAdapter adapterAt(Clock clock) {
        return new PdpSecurityApplicationAdapter(httpClient, PDP_URL, new ObjectMapper(), catalogPort, clock);
    }

    @SuppressWarnings("unchecked")
    private void respond(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        if (status == 200) {
            when(response.body()).thenReturn(body);
        }
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> response);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
