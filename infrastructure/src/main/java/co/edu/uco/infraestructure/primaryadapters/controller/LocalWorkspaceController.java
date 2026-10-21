package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.infraestructure.config.LocalWorkspaceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.LOCAL_WORKSPACE_PATH;

@RestController
@Profile("local-workspace")
final class LocalWorkspaceController {

    private final LocalWorkspaceProperties workspace;

    LocalWorkspaceController(final LocalWorkspaceProperties workspace) {
        this.workspace = workspace;
    }

    @GetMapping(path = LOCAL_WORKSPACE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<LocalWorkspaceProperties>> getWorkspace() {
        return Map.of("data", List.of(workspace));
    }
}
