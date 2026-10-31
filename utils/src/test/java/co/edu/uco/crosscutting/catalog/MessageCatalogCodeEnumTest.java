package co.edu.uco.crosscutting.catalog;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCatalogCodeEnumTest {

    @Test
    void allConstants_haveNonEmptyCode() {
        for (MessageCatalogCodeEnum constant : MessageCatalogCodeEnum.values()) {
            assertThat(constant.getCode()).isNotBlank();
        }
    }

    @Test
    void allCodes_areUnique() {
        Set<String> codes = new HashSet<>();
        for (MessageCatalogCodeEnum constant : MessageCatalogCodeEnum.values()) {
            assertThat(codes.add(constant.getCode()))
                    .as("Duplicate code: %s", constant.getCode())
                    .isTrue();
        }
    }

    @Test
    void technicalCodes_followTchPrefix() {
        assertThat(MessageCatalogCodeEnum.TCH_001.getCode()).isEqualTo("TCH_001");
        assertThat(MessageCatalogCodeEnum.TCH_058.getCode()).isEqualTo("TCH_058");
    }

    @Test
    void functionalCodes_followFunPrefix() {
        assertThat(MessageCatalogCodeEnum.FUN_001.getCode()).isEqualTo("FUN_001");
        assertThat(MessageCatalogCodeEnum.FUN_160.getCode()).isEqualTo("FUN_160");
    }
}
