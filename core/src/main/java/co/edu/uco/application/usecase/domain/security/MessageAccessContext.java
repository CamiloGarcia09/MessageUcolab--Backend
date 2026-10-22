package co.edu.uco.application.usecase.domain.security;

/**
 * Alcance confiable de una operacion de mensajes. {@code applicationId} se fija
 * tras validarlo contra la sesion del componente de seguridad. El ambiente es un dato de la operacion,
 * no parte del contexto de acceso.
 */
public record MessageAccessContext(
        String applicationId
) {
}
