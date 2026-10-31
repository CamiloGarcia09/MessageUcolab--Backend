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
} 