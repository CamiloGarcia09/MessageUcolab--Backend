package co.edu.uco.infraestructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.PULSAR_CONFIG_PREFIX;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = PULSAR_CONFIG_PREFIX)
public class PulsarProperties {
    private String serviceUrl;
    private String topicName;
    // Prefijo del topic de avisos; se publica en un topic por ambiente: <prefijo>-<environmentId>.
    private String eventsTopicPrefix = "persistent://public/default/message-events";
    // Interruptor para desactivar la publicacion de avisos sin tocar el resto del broker.
    private boolean eventsEnabled = true;
} 