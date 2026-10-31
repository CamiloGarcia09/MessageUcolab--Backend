package co.edu.uco.application.primaryports.dto.session;

import java.util.UUID;

public record WorkspaceUserDTO(UUID id, String email, String name) {
}
