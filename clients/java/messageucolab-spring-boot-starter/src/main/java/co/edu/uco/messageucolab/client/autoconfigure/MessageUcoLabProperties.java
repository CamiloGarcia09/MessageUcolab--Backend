package co.edu.uco.messageucolab.client.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "messageucolab")
public record MessageUcoLabProperties(
        String url,
        String applicationId,
        String environmentId,
        Duration timeout
) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    public MessageUcoLabProperties {
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
    }
}
