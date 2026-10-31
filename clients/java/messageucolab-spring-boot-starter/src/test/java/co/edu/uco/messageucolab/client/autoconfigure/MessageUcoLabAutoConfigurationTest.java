package co.edu.uco.messageucolab.client.autoconfigure;

import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.port.MessageFallbackPort;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import co.edu.uco.messageucolab.client.http.MessageUcoLabHttpAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("MessageUcoLabAutoConfiguration")
class MessageUcoLabAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageUcoLabAutoConfiguration.class,
                    WebClientAutoConfiguration.class))
            .withPropertyValues("messageucolab.url=https://messages.example",
                    "messageucolab.application-id=app-test", "messageucolab.environment-id=env-test");

    @Test
    @DisplayName("registra adaptador, fallback vacío y facade con propiedades enlazadas")
    void createsDefaultClientBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MessageReader.class).hasSingleBean(MessageFallbackPort.class);
            assertThat(context).hasSingleBean(MessageQueryPort.class);
            assertThat(context).hasSingleBean(WebClient.Builder.class);
            assertThat(context.getBean(MessageQueryPort.class)).isInstanceOf(MessageUcoLabHttpAdapter.class);
            MessageUcoLabProperties properties = context.getBean(MessageUcoLabProperties.class);
            assertAll(() -> assertThat(properties.applicationId()).isEqualTo("app-test"),
                    () -> assertThat(properties.environmentId()).isEqualTo("env-test"),
                    () -> assertThat(properties.timeout()).isEqualTo(java.time.Duration.ofSeconds(60)));
        });
    }

    @Test
    @DisplayName("respeta el timeout configurado explícitamente por el consumidor")
    void bindsExplicitTimeout() {
        contextRunner.withPropertyValues("messageucolab.timeout=7s").run(context ->
                assertThat(context.getBean(MessageUcoLabProperties.class).timeout())
                        .isEqualTo(java.time.Duration.ofSeconds(7)));
    }

    @Test
    @DisplayName("respeta los puertos definidos por el consumidor")
    void backsOffForConsumerPorts() {
        contextRunner.withUserConfiguration(CustomPortsConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(MessageReader.class);
            assertThat(context.getBean(MessageQueryPort.class)).isSameAs(CustomPortsConfiguration.QUERY);
            assertThat(context.getBean(MessageFallbackPort.class)).isSameAs(CustomPortsConfiguration.FALLBACK);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPortsConfiguration {
        private static final MessageQueryPort QUERY = new MessageQueryPort() {
            @Override
            public reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageDetails> findByCode(String code) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageDetails> findByCode(String code, String language) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<co.edu.uco.messageucolab.client.api.model.MessageCollection> findAll() {
                return reactor.core.publisher.Mono.empty();
            }
        };
        private static final MessageFallbackPort FALLBACK = code -> reactor.core.publisher.Mono.empty();

        @Bean
        MessageQueryPort customQueryPort() {
            return QUERY;
        }

        @Bean
        MessageFallbackPort customFallbackPort() {
            return FALLBACK;
        }
    }
}
