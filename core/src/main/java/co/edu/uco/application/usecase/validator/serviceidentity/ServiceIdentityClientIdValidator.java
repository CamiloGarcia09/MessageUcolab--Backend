package co.edu.uco.application.usecase.validator.serviceidentity;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

import java.util.regex.Pattern;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_233;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_234;

/**
 * Client ID de una identidad de servicio tal como lo muestra la consola de seguridad.
 */
public final class ServiceIdentityClientIdValidator {

    private static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:@-]{0,254}");

    private final CatalogPort catalog;

    public ServiceIdentityClientIdValidator(final CatalogPort catalog) {
        this.catalog = catalog;
    }

    public static boolean isValid(final String clientId) {
        return clientId != null && CLIENT_ID.matcher(clientId.trim()).matches();
    }

    /** Devuelve el client_id sin espacios o lanza el error de catalogo correspondiente. */
    public String validate(final String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw BusinessRuleException.buildUserException(catalog.getMessage(FUN_233.getCode()));
        }
        if (!isValid(clientId)) {
            throw BusinessRuleException.buildUserException(catalog.getMessage(FUN_234.getCode()));
        }
        return clientId.trim();
    }
}
