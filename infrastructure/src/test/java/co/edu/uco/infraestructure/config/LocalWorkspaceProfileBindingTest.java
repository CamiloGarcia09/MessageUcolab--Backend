package co.edu.uco.infraestructure.config;

import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.security.ExternalIdentityResolverPort;
import co.edu.uco.infraestructure.primaryadapters.interceptors.ExternalIdentityInterceptor;
import co.edu.uco.infraestructure.primaryadapters.interceptors.ActiveContextInterceptor;
import co.edu.uco.infraestructure.primaryadapters.interceptors.LocalWorkspaceInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalWorkspace profile binding")
class LocalWorkspaceProfileBindingTest {

    @Test
    @DisplayName("Sin perfil local-workspace: los filtros externos están presentes")
    void withoutLocalProfile_externalInterceptorsPresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        ActiveContextInterceptor.class,
                        ExternalIdentityInterceptor.class,
                        LocalWorkspaceInterceptor.class,
                        LocalWorkspaceConfig.class)
                .withBean(ActiveContextUseCaseFacade.class, Mockito::mock)
                .withBean(CatalogPort.class, Mockito::mock)
                .withBean(ExternalIdentityResolverPort.class, Mockito::mock)
                .withPropertyValues("components.security.simulated.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ActiveContextInterceptor.class)).isNotNull();
                    assertThat(context.getBean(ExternalIdentityInterceptor.class)).isNotNull();
                    assertThat(context).doesNotHaveBean(LocalWorkspaceInterceptor.class);
                    assertThat(context).doesNotHaveBean(LocalWorkspaceConfig.class);
                });
    }

    @Test
    @DisplayName("Con perfil local-workspace: el filtro externo es excluido")
    void withLocalProfile_localComponentsPresentLegacyAbsent() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        ActiveContextInterceptor.class,
                        ExternalIdentityInterceptor.class,
                        LocalWorkspaceInterceptor.class,
                        LocalWorkspaceConfig.class)
                .withBean(ActiveContextUseCaseFacade.class, Mockito::mock)
                .withBean(CatalogPort.class, Mockito::mock)
                .withBean(ExternalIdentityResolverPort.class, Mockito::mock)
                .withPropertyValues("spring.profiles.active=local-workspace")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LocalWorkspaceConfig.class)).isNotNull();
                    assertThat(context.getBean(LocalWorkspaceInterceptor.class)).isNotNull();
                    assertThat(context).doesNotHaveBean(ActiveContextInterceptor.class);
                    assertThat(context).doesNotHaveBean(ExternalIdentityInterceptor.class);
                });
    }

    @Test
    @DisplayName("Con perfil local-workspace bindea propiedades desde application-local-workspace.properties")
    void withLocalProfile_bindsPropertiesFromLocalWorkspaceFile() {
        new ApplicationContextRunner()
                .withUserConfiguration(LocalWorkspaceConfig.class)
                .withBean(ActiveContextUseCaseFacade.class, Mockito::mock)
                .withBean(CatalogPort.class, Mockito::mock)
                .withBean(ExternalIdentityResolverPort.class, Mockito::mock)
                .withInitializer(context -> {
                    org.springframework.core.env.ConfigurableEnvironment env = context.getEnvironment();
                    env.setActiveProfiles("local-workspace");
                    Properties props = new Properties();
                    try (InputStream is = getClass().getClassLoader().getResourceAsStream("application-local-workspace.properties")) {
                        if (is != null) {
                            props.load(is);
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                    Map<String, Object> map = new HashMap<>();
                    props.forEach((k, v) -> map.put(String.valueOf(k), String.valueOf(v)));
                    env.getPropertySources().addFirst(new MapPropertySource("local-workspace-test-props", map));
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    LocalWorkspaceProperties p = context.getBean(LocalWorkspaceProperties.class);
                    assertThat(p.organizationId().toString()).isEqualTo("8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40");
                    assertThat(p.organizationName()).isEqualTo("UCO");
                    assertThat(p.applicationId().toString()).isEqualTo("c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23");
                    assertThat(p.applicationName()).isEqualTo("Arquisoft");
                    assertThat(p.environmentId().toString()).isEqualTo("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82");
                    assertThat(p.environmentName()).isEqualTo("Develop");
                });
    }
}
