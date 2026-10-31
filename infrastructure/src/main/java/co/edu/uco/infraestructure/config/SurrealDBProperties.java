package co.edu.uco.infraestructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SURREAL_CONFIG_PREFIX;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix =SURREAL_CONFIG_PREFIX)
public class SurrealDBProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String namespace;
    private String database;
    private int maxConnections;

}
