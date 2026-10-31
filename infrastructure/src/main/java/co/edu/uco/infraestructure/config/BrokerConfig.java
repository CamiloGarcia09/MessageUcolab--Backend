package co.edu.uco.infraestructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

@Configuration
public class BrokerConfig {
    private PulsarClient client;
    private final PulsarProperties pulsarProperties;
    public BrokerConfig(PulsarProperties pulsarProperties) {
        this.pulsarProperties = pulsarProperties;
    }
    @PostConstruct
    public void init() throws PulsarClientException {
        this.client = PulsarClient.builder()
                .serviceUrl(pulsarProperties.getServiceUrl())
                .build();
    }
    @Bean
    public PulsarClient pulsarClient() {
        return this.client;
    }
    @PreDestroy
    public void cleanup() throws PulsarClientException {
        if (!isNullObject(this.client)) {
            this.client.close();
        }
    }
}