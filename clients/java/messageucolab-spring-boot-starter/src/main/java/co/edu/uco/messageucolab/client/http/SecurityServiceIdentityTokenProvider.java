package co.edu.uco.messageucolab.client.http;

import org.springframework.beans.factory.BeanFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Obtiene el token desde el {@code ServiceAccessTokenProvider} del starter de seguridad
 * ({@code security-pep-integration-spring-boot-starter}), que ya tienen las aplicaciones consumidoras.
 * Ese starter pide el token con {@code client_credentials}, lo guarda en caché y lo renueva antes de vencer.
 *
 * <p>El cliente no declara una dependencia de compilación con el starter de seguridad para no imponer su
 * versión ni su repositorio de paquetes: lo busca en el contexto de Spring al momento de cada consulta.</p>
 */
public final class SecurityServiceIdentityTokenProvider implements MessageUcoLabAccessTokenProvider {

    static final String SECURITY_TOKEN_PROVIDER = "co.edu.uco.seguridad.pep.starter.m2m.ServiceAccessTokenProvider";
    static final String NOT_CONFIGURED = "MessageUcoLab requiere la identidad de servicio de la aplicación: agregue "
            + "security-pep-integration-spring-boot-starter y configure security.pep.service-identity.enabled=true, "
            + "token-uri, client-id y client-secret.";

    private final BeanFactory beans;

    public SecurityServiceIdentityTokenProvider(BeanFactory beans) {
        this.beans = beans;
    }

    @Override
    public Mono<String> token() {
        return Mono.defer(() -> {
            Class<?> type;
            try {
                type = Class.forName(SECURITY_TOKEN_PROVIDER, false, getClass().getClassLoader());
            } catch (ClassNotFoundException missingStarter) {
                return Mono.error(new IllegalStateException(NOT_CONFIGURED, missingStarter));
            }
            Object provider = beans.getBeanProvider(type).getIfAvailable();
            if (provider == null) {
                return Mono.error(new IllegalStateException(NOT_CONFIGURED));
            }
            try {
                Method token = type.getMethod("token");
                return ((Mono<?>) token.invoke(provider)).map(String::valueOf);
            } catch (InvocationTargetException failure) {
                return Mono.error(failure.getCause());
            } catch (ReflectiveOperationException | ClassCastException failure) {
                return Mono.error(new IllegalStateException(NOT_CONFIGURED, failure));
            }
        });
    }
}
