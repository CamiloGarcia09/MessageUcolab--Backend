package co.edu.uco.crosscutting.catalog;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class BootstrapMessageCatalogEnumTest {

    @Test
    void allConstants_haveNonBlankFields() {
        for (BootstrapMessageCatalogEnum constant : BootstrapMessageCatalogEnum.values()) {
            assertAll("constant %s", () -> assertThat(constant.getCode()).isNotBlank(),
                    () -> assertThat(constant.getTitle()).isNotBlank(),
                    () -> assertThat(constant.getContent()).isNotBlank(),
                    () -> assertThat(constant.getType()).isNotBlank(),
                    () -> assertThat(constant.getCategory()).isNotBlank());
        }
    }

    @Test
    void allCodes_areUniqueAndMatchConstantName() {
        Set<String> codes = new HashSet<>();
        for (BootstrapMessageCatalogEnum constant : BootstrapMessageCatalogEnum.values()) {
            assertAll("constant %s", () -> assertThat(codes.add(constant.getCode()))
                            .as("Duplicate code: %s", constant.getCode())
                            .isTrue(),
                    () -> assertThat(constant.getCode()).isEqualTo(constant.name()));
        }
    }

    @Test
    void findByCode_returnsConstant_whenExists() {
        Optional<BootstrapMessageCatalogEnum> found = BootstrapMessageCatalogEnum.findByCode("TCH_007");

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("The message key is null");
    }

    @Test
    void findByCode_returnsEmpty_whenNotExists() {
        assertThat(BootstrapMessageCatalogEnum.findByCode("TCH_010")).isEmpty();
    }

    @Test
    void findByCode_returnsEmpty_whenCodeIsBlank() {
        assertThat(BootstrapMessageCatalogEnum.findByCode(" ")).isEmpty();
        assertThat(BootstrapMessageCatalogEnum.findByCode(null)).isEmpty();
    }

    @Test
    void exists_returnsTrue_whenCodeExists() {
        assertThat(BootstrapMessageCatalogEnum.exists("FUN_013")).isTrue();
    }

    @Test
    void exists_returnsFalse_whenCodeNotExists() {
        assertThat(BootstrapMessageCatalogEnum.exists("FUN_001")).isFalse();
    }

    @Test
    void count_includesApplicationScopedRepositoryErrorCatalogEntry() {
        assertThat(BootstrapMessageCatalogEnum.values()).hasSize(36);
    }
}
