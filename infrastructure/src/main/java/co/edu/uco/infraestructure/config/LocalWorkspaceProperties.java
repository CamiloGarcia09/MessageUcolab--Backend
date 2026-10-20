package co.edu.uco.infraestructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "components.workspace.local")
public record LocalWorkspaceProperties(
        UUID organizationId,
        String organizationName,
        UUID applicationId,
        String applicationName,
        UUID environmentId,
        String environmentName
) {
}
