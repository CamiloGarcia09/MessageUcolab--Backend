package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.infraestructure.config.LocalWorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("LocalWorkspaceController")
class LocalWorkspaceControllerTest {

    private LocalWorkspaceProperties properties;
    private LocalWorkspaceController controller;

    @BeforeEach
    void setUp() {
        properties = new LocalWorkspaceProperties(
                UUID.fromString("8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40"),
                "UCO",
                UUID.fromString("c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23"),
                "Arquisoft",
                UUID.fromString("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"),
                "Develop");
        controller = new LocalWorkspaceController(properties);
    }

    @Nested
    @DisplayName("getWorkspace")
    class GetWorkspace {

        @Test
        @DisplayName("Retorna contrato JSON con estructura data:[properties]")
        void returnsDataArrayWithProperties() {
            Map<String, List<LocalWorkspaceProperties>> result = controller.getWorkspace();

            assertAll(
                    () -> assertThat(result).containsKey("data"),
                    () -> assertThat(result.get("data")).isNotNull(),
                    () -> assertThat(result.get("data")).hasSize(1),
                    () -> assertThat(result.get("data").get(0)).isEqualTo(properties));
        }

        @Test
        @DisplayName("Retorna las propiedades configuradas exactamente")
        void returnsConfiguredPropertiesExactly() {
            Map<String, List<LocalWorkspaceProperties>> result = controller.getWorkspace();
            LocalWorkspaceProperties returned = result.get("data").get(0);

            assertAll(
                    () -> assertThat(returned.organizationId())
                            .isEqualTo(UUID.fromString("8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40")),
                    () -> assertThat(returned.organizationName()).isEqualTo("UCO"),
                    () -> assertThat(returned.applicationId())
                            .isEqualTo(UUID.fromString("c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23")),
                    () -> assertThat(returned.applicationName()).isEqualTo("Arquisoft"),
                    () -> assertThat(returned.environmentId())
                            .isEqualTo(UUID.fromString("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82")),
                    () -> assertThat(returned.environmentName()).isEqualTo("Develop"));
        }
    }
}
