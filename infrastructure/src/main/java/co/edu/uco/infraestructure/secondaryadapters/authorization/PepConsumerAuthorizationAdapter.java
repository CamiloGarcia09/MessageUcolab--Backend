package co.edu.uco.infraestructure.secondaryadapters.authorization;

import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * La autorizacion entre aplicaciones la decide el PEP: solo llega aqui una identidad de servicio a la
 * que MessageUcoLab concedio la ruta de consumo en el componente de seguridad. Seguridad no sabe a que
 * aplicacion de MessageUcoLab corresponde cada identidad, asi que no se agrega otra restriccion; si se
 * necesita, se crea otro adaptador y se cambia el modo, sin tocar el core.
 */
@Component
@ConditionalOnProperty(prefix = "messageucolab.consumer.authorization", name = "mode",
        havingValue = "pep", matchIfMissing = true)
public class PepConsumerAuthorizationAdapter implements ConsumerAuthorizationPort {

    @Override
    public boolean canConsume(String applicationId, String environmentId) {
        return true;
    }
}
