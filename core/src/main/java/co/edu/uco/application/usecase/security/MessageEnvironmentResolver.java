package co.edu.uco.application.usecase.security;

import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public interface MessageEnvironmentResolver {

    String resolve(MessageAccessContext context, String environmentId);

    /**
     * Aplicacion sobre la que opera la peticion. Los mensajes pertenecen a la aplicacion y el
     * ambiente es un dato de cada mensaje, no parte del contexto.
     */
    String resolveApplication(MessageAccessContext context);
}
