package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.PublicMessageAccess;
import co.edu.uco.application.usecase.validator.consumer.PublicMessageAccessCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * Las consultas publicas no tienen sesion: la aplicacion y el ambiente llegan del consumidor.
 * Se valida que existan y que el ambiente pertenezca a la aplicacion, y luego se pregunta
 * al puerto de autorizacion si esa aplicacion puede consumir.
 */
public final class PublicMessageAccessResolver {

    private final PublicMessageAccessCompositeValidator validator;
    private final ConsumerAuthorizationPort authorization;
    private final CatalogPort catalogPort;

    public PublicMessageAccessResolver(final PublicMessageAccessCompositeValidator validator,
                                       final ConsumerAuthorizationPort authorization,
                                       final CatalogPort catalogPort) {
        this.validator = validator;
        this.authorization = authorization;
        this.catalogPort = catalogPort;
    }

    public PublicMessageAccess resolve(final String applicationId, final String environmentId) {
        validator.validate(applicationId, environmentId);
        var access = new PublicMessageAccess(getUUIDFromString(applicationId).toString(),
                getUUIDFromString(environmentId).toString());
        if (!authorization.canConsume(access.applicationId(), access.environmentId())) {
            throw ForbiddenException.buildUserException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_221.getCode()));
        }
        return access;
    }
}
