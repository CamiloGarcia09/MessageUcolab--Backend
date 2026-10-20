package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.infraestructure.config.LocalWorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalWorkspaceInterceptor headers/cookies preservation")
class LocalWorkspaceInterceptorHeadersTest {

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

    @Test
    @DisplayName("Conserva Authorization, Cookie y Token en request; solo fija ambiente y elimina identidad antigua")
    void preservesHeadersAndCookies_setsOnlyEnvironment() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/messageucolab/v1/messages")
                .header("Authorization", "Bearer test-token")
                .header("Token", "legacy-token")
                .header("Cookie", "session=abc123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, new Object());

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

        assertAll(
                () -> assertThat(exchange.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token"),
                () -> assertThat(exchange.getRequest().getHeaders().getFirst("Token")).isEqualTo("legacy-token"),
                () -> assertThat(exchange.getRequest().getHeaders().getFirst("Cookie")).isEqualTo("session=abc123"),
                () -> assertThat((String) exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE))
                        .isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"),
                () -> assertThat((Object) exchange.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).isNull(),
                () -> assertThat(exchange.getResponse().getStatusCode()).isNull()
        );
        verify(chain).filter(exchange);
    }
}
