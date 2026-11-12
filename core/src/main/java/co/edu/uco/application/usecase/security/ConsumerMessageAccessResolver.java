package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.application.usecase.domain.security.ConsumerMessageAccess;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityClientIdValidator;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;

import java.util.Optional;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_221;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_232;

/**
 * Una aplicacion consumidora llama con el token de su identidad de servicio, sin sesion BFF: el PEP ya
 * autorizo esa identidad para la ruta. La aplicacion y el ambiente salen del client_id del token, que el
 * propietario asocio a un ambiente; la aplicacion consumidora no los envia.
 */
public final class ConsumerMessageAccessResolver {

    private final ServiceIdentityRepository identities;
    private final ConsumerAuthorizationPort authorization;
    private final CatalogPort catalogPort;

    public ConsumerMessageAccessResolver(final ServiceIdentityRepository identities,
                                         final ConsumerAuthorizationPort authorization,
                                         final CatalogPort catalogPort) {
        this.identities = identities;
        this.authorization = authorization;
        this.catalogPort = catalogPort;
    }

    public ConsumerMessageAccess resolve(final String clientId) {
        Optional<ServiceIdentityData> identity = ServiceIdentityClientIdValidator.isValid(clientId)
                ? identities.findByClientId(clientId.trim())
                : Optional.empty();
        var access = identity
                .map(found -> new ConsumerMessageAccess(found.applicationId(), found.environmentId()))
                .orElseThrow(() -> ForbiddenException.buildUserException(catalogPort.getMessage(FUN_232.getCode())));
        if (!authorization.canConsume(access.applicationId(), access.environmentId())) {
            throw ForbiddenException.buildUserException(catalogPort.getMessage(FUN_221.getCode()));
        }
        return access;
    }
}
