package co.edu.uco.application.usecase.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogNames")
class CatalogNamesTest {

    @ParameterizedTest
    @ValueSource(strings = {"Activo", "ACTIVO", "Active", "active"})
    @DisplayName("recognizes Spanish and legacy active names")
    void isActive_recognizesActiveNames(String name) {
        assertThat(CatalogNames.isActive(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Inactivo", "Inactive", "Reactivado", "", "  ", "Activa"})
    @NullSource
    @DisplayName("rejects inactive, blank, null, and merely similar names")
    void isActive_rejectsNonActiveNames(String name) {
        assertThat(CatalogNames.isActive(name)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Production", "Produccion", "Producción", "PRODUCCIÓN"})
    @DisplayName("recognizes production names regardless of case or accent variant")
    void isProduction_recognizesProductionNames(String name) {
        assertThat(CatalogNames.isProduction(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Preproduction", "Produccion local", "Develop", "", "  "})
    @NullSource
    @DisplayName("rejects null, blank, and partial production names")
    void isProduction_rejectsOtherNames(String name) {
        assertThat(CatalogNames.isProduction(name)).isFalse();
    }

    @Test
    @DisplayName("matches production variants while keeping other environment types exact")
    void sameEnvironmentType_matchesProductionVariantsOnly() {
        assertThat(CatalogNames.sameEnvironmentType("Production", "Producción")).isTrue();
        assertThat(CatalogNames.sameEnvironmentType("Develop", "DEVELOP")).isTrue();
        assertThat(CatalogNames.sameEnvironmentType("Develop", "Development")).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("does not match environments when either identity is null or blank")
    void sameEnvironmentType_rejectsMissingIdentity(String absent) {
        assertThat(CatalogNames.sameEnvironmentType(absent, "Production")).isFalse();
        assertThat(CatalogNames.sameEnvironmentType("Production", absent)).isFalse();
    }
}
