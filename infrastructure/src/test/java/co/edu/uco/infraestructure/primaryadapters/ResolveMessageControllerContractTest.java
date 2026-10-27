package co.edu.uco.infraestructure.primaryadapters;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveMessageControllerContractTest {
    @Test
    void route_exposesConsumerPathWithoutLegacyResolveAlias() throws Exception {
        var method = ResolveMessageController.class.getMethod("resolve", String.class,
                org.springframework.web.server.ServerWebExchange.class);
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/consumer");
        assertThat(method.getParameterTypes()[1]).isEqualTo(org.springframework.web.server.ServerWebExchange.class);
        assertThat(method.getReturnType()).isEqualTo(reactor.core.publisher.Mono.class);
    }

}
