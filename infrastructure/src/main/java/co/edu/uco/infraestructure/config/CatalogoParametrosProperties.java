package co.edu.uco.infraestructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Conexión con el componente de Parámetros (Catálogo de Parámetros).
 *
 * <p>El token se crea en el frontend del Catálogo (<b>Tokens → Nuevo</b>) y queda ligado a una aplicación y a un
 * ambiente. Si falta la URL, el ID del token o su secreto, MessageUcoLab no consulta el Catálogo y usa sus
 * valores por defecto. El secreto llega solo por variable de entorno o gestor de secretos.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "catalogo.parametros")
public class CatalogoParametrosProperties {

    /** Por ejemplo {@code http://host.docker.internal:8081/catalogo-parametros/api/v1}. */
    private String baseUrl;
    private String tokenId;
    private String secreto;
    /** Cada cuánto se vuelven a consultar los parámetros. */
    private Duration cacheTtl = Duration.ofSeconds(60);
    private Duration timeout = Duration.ofSeconds(3);

    public boolean isConfigured() {
        return hasText(baseUrl) && hasText(tokenId) && hasText(secreto);
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
