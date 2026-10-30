package co.edu.uco.infraestructure.secondaryadapters.authorization;

import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Acceso abierto acordado con el profesor mientras seguridad no autoriza aplicacion a aplicacion.
 * Cuando exista ese servicio se agrega otro adaptador y se cambia el modo, sin tocar el core.
 */
@Component
@ConditionalOnProperty(prefix = "messageucolab.consumer.authorization", name = "mode",
        havingValue = "public", matchIfMissing = true)
public class OpenConsumerAuthorizationAdapter implements ConsumerAuthorizationPort {

    @Override
    public boolean canConsume(String applicationId, String environmentId) {
        return true;
    }
}
