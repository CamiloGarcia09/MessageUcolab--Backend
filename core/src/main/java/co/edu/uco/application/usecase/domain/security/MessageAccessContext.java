package co.edu.uco.application.usecase.domain.security;

public record MessageAccessContext(
        String workspaceEnvironmentId,
        ExternalIdentity externalIdentity
) {
}
