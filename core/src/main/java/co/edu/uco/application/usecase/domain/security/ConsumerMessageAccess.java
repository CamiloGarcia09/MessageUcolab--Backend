package co.edu.uco.application.usecase.domain.security;

/**
 * Aplicacion y ambiente validados de una aplicacion consumidora autenticada con su identidad de servicio, ya normalizados como UUID.
 */
public record ConsumerMessageAccess(String applicationId, String environmentId) {

    public MessageAccessContext context() {
        return new MessageAccessContext(applicationId);
    }
}
