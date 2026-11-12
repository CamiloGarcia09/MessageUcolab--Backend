package co.edu.uco.infraestructure.primaryadapters.consumer;

import co.edu.uco.application.usecase.domain.security.ConsumerMessageAccess;
import co.edu.uco.application.usecase.security.ConsumerMessageAccessResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;

/**
 * Aplicacion y ambiente de una ruta de consumo. Con sesion BFF los fijo el interceptor de sesion; con
 * identidad de servicio salen del client_id del token. Se resuelve en el controlador, despues del PEP,
 * para que una identidad no autenticada reciba siempre el 401 del PEP.
 */
@Component
public final class ConsumerRequestAccess {

    private final ConsumerMessageAccessResolver resolver;
    private final ServiceIdentityClientIdReader clientIdReader;

    public ConsumerRequestAccess(final ConsumerMessageAccessResolver resolver,
                                 final ServiceIdentityClientIdReader clientIdReader) {
        this.resolver = resolver;
        this.clientIdReader = clientIdReader;
    }

    public ConsumerMessageAccess resolve(final ServerWebExchange exchange) {
        String sessionApplicationId = exchange.getAttribute(APPLICATION_ID_ATTRIBUTE);
        if (sessionApplicationId != null) {
            return new ConsumerMessageAccess(sessionApplicationId, exchange.getAttribute(ENVIRONMENT_ID_ATTRIBUTE));
        }
        return resolver.resolve(clientIdReader.read(exchange));
    }
}
