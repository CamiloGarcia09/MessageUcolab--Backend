package co.edu.uco.infraestructure.config;
import org.junit.jupiter.api.*; import org.mockito.*; import org.springframework.mock.http.server.reactive.MockServerHttpRequest; import org.springframework.mock.web.server.MockServerWebExchange; import org.springframework.web.server.WebFilterChain; import reactor.core.publisher.Mono; import static org.mockito.Mockito.*; import static org.assertj.core.api.Assertions.assertThat; import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;
class LoggingConfigTest {
 @Mock WebFilterChain chain; LoggingConfig filter;
 @BeforeEach void setUp(){MockitoAnnotations.openMocks(this);filter=new LoggingConfig();when(chain.filter(any())).thenReturn(Mono.empty());}
 @Test void filter_copiesCorrelationId(){var e=MockServerWebExchange.from(MockServerHttpRequest.get("/").header(CORRELATION_ID,"id-1").build());filter.filter(e,chain).block();assertThat(e.getResponse().getHeaders().getFirst(CORRELATION_ID)).isEqualTo("id-1");verify(chain).filter(e);}
 @Test void filter_generatesCorrelationId(){var e=MockServerWebExchange.from(MockServerHttpRequest.get("/").build());filter.filter(e,chain).block();assertThat(e.getResponse().getHeaders().getFirst(CORRELATION_ID)).isNotBlank();}
}
