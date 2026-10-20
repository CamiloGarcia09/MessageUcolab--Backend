package co.edu.uco.infraestructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local-workspace")
@EnableConfigurationProperties(LocalWorkspaceProperties.class)
public class LocalWorkspaceConfig {
}
