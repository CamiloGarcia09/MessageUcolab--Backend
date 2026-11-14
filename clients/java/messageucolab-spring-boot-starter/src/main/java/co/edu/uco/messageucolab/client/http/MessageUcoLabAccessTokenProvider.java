package co.edu.uco.messageucolab.client.http;

import reactor.core.publisher.Mono;

/**
 * Entrega el Bearer token con el que la aplicación consumidora se identifica ante MessageUcoLab.
 *
 * <p>Por defecto el starter usa la identidad de servicio que la aplicación ya configuró en el starter de
 * seguridad ({@code security.pep.service-identity.*}). Una aplicación puede registrar su propio bean de
 * este tipo si obtiene el token de otra forma</p>
 */
@FunctionalInterface
public interface MessageUcoLabAccessTokenProvider {

    Mono<String> token();
}
