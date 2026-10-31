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
    private boolean enabled = true;
    private String provider = "ollama";
    private String apiKey;
    private String baseUrl = "http://ollama:11434";
    private String modelName = "llama3.2";
    private double temperature = 0.1;
    private int maxRetries = 0;
    private long timeoutSeconds = 30;
}
