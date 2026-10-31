package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.WEB_CONFIG_API_APPLICATION;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.WEB_CONFIG_API_CATALOG;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.WEB_CONFIG_API_MESSAGES;

@DisplayName("WebConfig")
class WebConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(WebConfig.class);

    @Test
    @DisplayName("Se registra como bean de configuración reactiva")
    void registersAsConfigurationBean() {
        runner.run(context -> assertThat(context).hasSingleBean(WebConfig.class));
    }

    @Test
    @DisplayName("Define los patrones de rutas protegidas de la API v1")
    void definesProtectedResourcePatterns() {
        assertAll(
                () -> assertThat(WEB_CONFIG_API_MESSAGES).isEqualTo("/messageucolab/v1/messages/**"),
                () -> assertThat(WEB_CONFIG_API_APPLICATION).isEqualTo("/messageucolab/v1/application/**/message/*"),
                () -> assertThat(WEB_CONFIG_API_CATALOG).isEqualTo("/messageucolab/v1/catalog/**"));
    }
}
