package co.edu.uco.application.usecase.domain.security;

/**
 * Aplicacion y ambiente validados de una consulta publica, ya normalizados como UUID.
 */
public record PublicMessageAccess(String applicationId, String environmentId) {

    public MessageAccessContext context() {
        return new MessageAccessContext(applicationId);
    }
}
