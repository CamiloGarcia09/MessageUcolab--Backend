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
    CorsWebFilter corsWebFilter(
            @Value("${messageucolab.cors.admin-origins}") List<String> adminOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(adminOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-Correlation-Id", "X-Application-Id", "X-Environment-Id"));
        cors.setExposedHeaders(List.of("X-Decision-Id"));
        cors.setAllowCredentials(true);

        // Los backends consumidores llaman con su identidad de servicio y no usan CORS.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
