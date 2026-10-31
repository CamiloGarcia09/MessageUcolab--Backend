package co.edu.uco.infraestructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
class CorsConfig {

    @Bean
    CorsWebFilter corsWebFilter(@Value("${messageucolab.cors.public-origins}") List<String> publicOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5174"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-Correlation-Id", "X-Application-Id", "X-Environment-Id"));
        cors.setExposedHeaders(List.of("X-Decision-Id"));
        cors.setAllowCredentials(true);

        // CORS solo limita a los navegadores: los backends consumidores leen la ruta publica sin el.
        // Los fronts que la leen desde el navegador se declaran uno a uno, sin comodin.
        CorsConfiguration publicCors = new CorsConfiguration();
        publicCors.setAllowedOrigins(publicOrigins);
        publicCors.setAllowedMethods(List.of("GET", "OPTIONS"));
        publicCors.setAllowedHeaders(List.of("Accept", "X-Correlation-Id", "X-Application-Id", "X-Environment-Id"));
        publicCors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // El primer patron que coincide gana: la ruta publica va antes que la general.
        source.registerCorsConfiguration("/messageucolab/v1/public/**", publicCors);
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
