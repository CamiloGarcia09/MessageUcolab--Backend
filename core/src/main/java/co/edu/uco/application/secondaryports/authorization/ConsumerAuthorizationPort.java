package co.edu.uco.application.secondaryports.authorization;

/**
 * Decide si una aplicacion consumidora puede consultar los mensajes publicos de un ambiente.
 * Mientras seguridad no resuelva la autorizacion entre aplicaciones el acceso es abierto;
 * despues este puerto consultara a seguridad sin cambiar el contrato de los consumidores.
 */
public interface ConsumerAuthorizationPort {

    boolean canConsume(String applicationId, String environmentId);
}
