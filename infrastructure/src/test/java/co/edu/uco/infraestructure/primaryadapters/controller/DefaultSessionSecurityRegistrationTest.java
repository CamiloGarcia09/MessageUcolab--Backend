package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.infraestructure.primaryadapters.interceptors.SessionApplicationInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Default BFF session security registration")
class DefaultSessionSecurityRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                try {
                    context.getEnvironment().getPropertySources().addLast(
                            new ResourcePropertySource("classpath:application.properties"));
                } catch (IOException failure) {
                    throw new IllegalStateException("Could not load application.properties", failure);
                }
            })
            .withBean(SessionApplicationUseCaseFacade.class, () -> mock(SessionApplicationUseCaseFacade.class))
            .withBean(PersonalWorkspaceUseCaseFacade.class, () -> mock(PersonalWorkspaceUseCaseFacade.class))
            .withBean(PresenterPort.class, () -> mock(PresenterPort.class))
            .withUserConfiguration(SessionApplicationInterceptor.class, SessionApplicationControllerImpl.class);

    @Test
    @DisplayName("Without an active profile registers session API and PDP property")
    void defaultStartup_registersSessionComponentsAndLoadsPdpUrl() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(context).hasSingleBean(SessionApplicationInterceptor.class);
            assertThat(context).hasSingleBean(SessionApplicationControllerImpl.class);
            assertThat(context.getEnvironment().getProperty("components.security.session.pdp-url"))
                    .isEqualTo("http://localhost:18080");
            assertThat(context.getEnvironment().containsProperty("components.security.simulated.enabled")).isFalse();
        });
    }

    @Test
    @DisplayName("Azure profile retains normal session API registration")
    void azureProfile_keepsSessionComponentsRegistered() {
        contextRunner.withPropertyValues("spring.profiles.active=azure").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SessionApplicationInterceptor.class);
            assertThat(context).hasSingleBean(SessionApplicationControllerImpl.class);
            assertThat(context.getEnvironment().getProperty("components.security.session.pdp-url"))
                    .isEqualTo("http://localhost:18080");
        });
    }

}
