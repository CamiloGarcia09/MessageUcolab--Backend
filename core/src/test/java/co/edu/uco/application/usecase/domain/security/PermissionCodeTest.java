package co.edu.uco.application.usecase.domain.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PermissionCodeTest {

    @Test
    @DisplayName("Expone los permisos soportados en un orden estable")
    void values_containsSupportedPermissionsInStableOrder() {
        assertThat(PermissionCode.values()).containsExactly(
                PermissionCode.CONTEXT_SELECT,
                PermissionCode.MESSAGE_READ,
                PermissionCode.MESSAGE_CREATE,
                PermissionCode.MESSAGE_UPDATE,
                PermissionCode.MESSAGE_DELETE,
                PermissionCode.MESSAGE_TRANSLATE,
                PermissionCode.APPLICATION_CREATE,
                PermissionCode.FUNCTIONALITY_CREATE
        );
    }

    @Test
    @DisplayName("No declara el permiso eliminado de creación manual de ambientes")
    void values_doesNotContainRemovedEnvironmentCreationPermission() {
        List<String> permissionNames = Arrays.stream(PermissionCode.values())
                .map(PermissionCode::name)
                .toList();

        assertAll(
                () -> assertThat(permissionNames).doesNotContain("ENVIRONMENT_CREATE"),
                () -> assertThat(permissionNames).contains("APPLICATION_CREATE", "FUNCTIONALITY_CREATE"));
    }
}
