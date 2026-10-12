package co.edu.uco.infraestructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "components.security.simulated", name = "enabled", havingValue = "true")
public class ExternalIdentityWebConfig {
}
