package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptHeaderInterceptor")
class AcceptHeaderInterceptorTest {

    @Mock
    private SerializerRegistry registry;
    @Mock
    private CatalogPort catalog;
    @Mock
    private LoggingPortFactory factory;
    @Mock
    private LoggingPort log;
    @Mock
    private SerializerType serializer;
    @Mock
    private WebFilterChain chain;

    private AcceptHeaderInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(factory.getLogger(AcceptHeaderInterceptor.class)).thenReturn(log);
        interceptor = new AcceptHeaderInterceptor(registry, catalog, factory);
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("Continúa la cadena cuando el Accept es soportado")
        void supportedAccept_continuesChain() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").header("Accept", "application/json").build());
            when(registry.getSerializerForMediaType("application/json")).thenReturn(serializer);
            when(serializer.supports("application/json")).thenReturn(true);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Usa JSON por defecto cuando no llega cabecera Accept")
        void absentAccept_fallsBackToDefaultJson() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").build());
            when(registry.getSerializerForMediaType("application/json")).thenReturn(serializer);
            when(serializer.supports("application/json")).thenReturn(true);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("Responde 406, fija el content-type y no continúa cuando el Accept no es soportado")
        void unsupportedAccept_returns406WithoutContinuing() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/messageucolab/v1/messages").header("Accept", "application/pdf").build());
            when(registry.getSerializerForMediaType("application/pdf")).thenReturn(serializer);
            when(serializer.supports("application/pdf")).thenReturn(false);
            when(serializer.getSupportedContentType()).thenReturn("application/json");
            when(catalog.getMessage("TCH_023")).thenReturn("media type no soportado");

            StepVerifier.create(interceptor.filter(exchange, chain)).verifyComplete();

            assertAll(
                    () -> assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE),
                    () -> assertThat(exchange.getResponse().getHeaders().getContentType())
                            .isEqualTo(MediaType.APPLICATION_JSON));
            verify(chain, never()).filter(exchange);
            verify(log).error("media type no soportado", "application/pdf");
        }
    }
}
