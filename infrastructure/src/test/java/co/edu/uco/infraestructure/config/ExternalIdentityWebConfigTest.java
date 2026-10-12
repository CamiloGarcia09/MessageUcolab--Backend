package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class ExternalIdentityWebConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ExternalIdentityWebConfig.class);
    @Test void disabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ExternalIdentityWebConfig.class));
    }
    @Test void enabledWhenConfigured() {
        runner.withPropertyValues("components.security.simulated.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ExternalIdentityWebConfig.class));
    }
}
