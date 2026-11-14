package co.edu.uco.infraestructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "translation.ai")
public class TranslationAiProperties {
    /**
     * Valor por defecto. Manda el parámetro {@link #parameterName} del componente de Parámetros cuando existe;
     * este valor solo se usa si el parámetro no está configurado o el componente no responde.
     */
    private boolean enabled = false;
    /** Nombre del parámetro que, en el componente de Parámetros, habilita o deshabilita la traducción. */
    private String parameterName = "TRADUCCION_HABILITADA";
    private String provider = "ollama";
    private String apiKey;
    private String baseUrl = "http://ollama:11434";
    private String modelName = "llama3.2";
    private double temperature = 0.1;
    private int maxRetries = 0;
    private long timeoutSeconds = 30;
    /** Traducciones que pueden estar en curso a la vez; las que excedan este número reciben el texto original. */
    private int maxConcurrentRequests = 2;
}
