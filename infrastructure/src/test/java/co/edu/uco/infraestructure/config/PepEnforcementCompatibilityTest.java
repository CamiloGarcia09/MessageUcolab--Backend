package co.edu.uco.infraestructure.config;

import co.edu.uco.seguridad.pep.starter.PepEnforcementAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PEP starter compatibility with Spring Framework")
class PepEnforcementCompatibilityTest {

    @Test
    @DisplayName("Filtro PEP real responde 401 con headers ausentes sin alcanzar el negocio")
    void filter_withEmptyHeaders_returnsUnauthorizedBeforeCallingPdpOrBusiness() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PepEnforcementAutoConfiguration.class))
                .withPropertyValues(
                        "security.enabled=true",
                        "security.pep.enforcement.enabled=true",
                        "security.pep.enforcement.pep-url=http://localhost:18081",
                        "security.pep.enforcement.application-name=MessageUcoLab",
                        "security.pep.enforcement.environment=local",
                        "security.pep.enforcement.application-credential=test-only-credential",
                        "security.pep.enforcement.allow-insecure-http=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WebFilter.class);
                    WebFilter filter = context.getBean(WebFilter.class);
                    assertThat(filter.getClass().getSimpleName()).isEqualTo("PepEnforcementWebFilter");

                    var exchange = MockServerWebExchange.from(
                            org.springframework.mock.http.server.reactive.MockServerHttpRequest
                                    .get("/messageucolab/v1/messages").build());
                    AtomicBoolean businessInvoked = new AtomicBoolean();

                    Mono<Void> filtered = filter.filter(exchange, ignored -> {
                        businessInvoked.set(true);
                        return Mono.empty();
                    });
                    filtered.block();

                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(businessInvoked).isFalse();
                });
    }
}
