package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.infraestructure.config.InfrastructureConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ManualEnvironmentCreationContractTest {

    @Nested
    @DisplayName("El flujo manual de creación de ambientes fue retirado")
    class RemovedManualFlow {

        @Test
        @DisplayName("No existe el controller ni su implementación de creación de ambientes")
        void createEnvironmentControllers_areNoLongerDeclared() {
            assertAll(
                    () -> assertThatThrownBy(() -> Class.forName(
                                    "co.edu.uco.infraestructure.primaryadapters.CreateEnvironmentController"))
                            .isInstanceOf(ClassNotFoundException.class),
                    () -> assertThatThrownBy(() -> Class.forName(
                                    "co.edu.uco.infraestructure.primaryadapters.controller.CreateEnvironmentControllerImpl"))
                            .isInstanceOf(ClassNotFoundException.class));
        }

        @Test
        @DisplayName("La constante de ruta del interceptor de ambientes ya no está declarada")
        void infrastructureConstant_doesNotDeclareEnvironmentPathAnymore() {
            assertThatThrownBy(() -> InfrastructureConstant.class.getDeclaredField("WEB_CONFIG_API_ENVIRONMENT"))
                    .isInstanceOf(NoSuchFieldException.class);
        }
    }

    @Nested
    @DisplayName("El contrato OpenAPI mantiene la creación de ambientes dentro de la aplicación")
    class OpenApiContract {

        @Test
        @DisplayName("No publica operación, ruta ni esquema de creación manual de ambientes")
        void openApiSpecification_doesNotExposeManualEnvironmentCreation() {
            String openApi = readStaticResource("static/openapi.yaml");

            assertAll(
                    () -> assertThat(openApi).doesNotContain("/messageucolab/v1/application/environment"),
                    () -> assertThat(openApi).doesNotContain("operationId: createEnvironment"),
                    () -> assertThat(openApi).doesNotContain("CreateEnvironmentRequest"),
                    () -> assertThat(openApi).contains("/messageucolab/v1/application:"),
                    () -> assertThat(openApi).contains("operationId: createApplication"));
        }
    }

    private static String readStaticResource(final String path) {
        try (InputStream input = ManualEnvironmentCreationContractTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            assertThat(input).as(path + " must be on the test classpath").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
