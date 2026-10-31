package co.edu.uco.messageucolab.client.autoconfigure;

import co.edu.uco.messageucolab.client.api.DefaultMessageReader;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.port.MessageFallbackPort;
import co.edu.uco.messageucolab.client.api.port.MessageQueryPort;
import co.edu.uco.messageucolab.client.http.MessageUcoLabHttpAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(MessageUcoLabProperties.class)
public class MessageUcoLabAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MessageQueryPort messageQueryPort(WebClient.Builder builder, MessageUcoLabProperties properties) {
        return new MessageUcoLabHttpAdapter(builder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    MessageFallbackPort messageFallbackPort() {
        return code -> Mono.empty();
    }

    @Bean
    @ConditionalOnMissingBean
    MessageReader messageReader(MessageQueryPort queryPort, MessageFallbackPort fallbackPort) {
        return new DefaultMessageReader(queryPort, fallbackPort);
    }
}
