package co.edu.uco.application.secondaryports.authorization;

/**
 * Decide si una aplicacion consumidora puede consultar los mensajes de un ambiente.
 * La identidad de servicio ya fue autorizada por el PEP para la ruta; este puerto permite agregar
 * despues una restriccion por aplicacion sin cambiar el contrato de los consumidores.
 */
public interface ConsumerAuthorizationPort {

    boolean canConsume(String applicationId, String environmentId);
}
