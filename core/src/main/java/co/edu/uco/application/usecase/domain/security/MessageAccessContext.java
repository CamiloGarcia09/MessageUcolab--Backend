package co.edu.uco.application.usecase.domain.security;

/**
 * Alcance confiable de una operacion de mensajes. {@code applicationId} lo fija el modo sesion
 * tras validarlo contra el componente de seguridad; {@code workspaceEnvironmentId} es el destino
 * fijo de local-workspace o el ambiente explicito validado para resolver mensajes.
 */
public record MessageAccessContext(
        String workspaceEnvironmentId,
        ExternalIdentity externalIdentity,
        String applicationId
) {

    public MessageAccessContext(String workspaceEnvironmentId, ExternalIdentity externalIdentity) {
        this(workspaceEnvironmentId, externalIdentity, null);
    }
}
