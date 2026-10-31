package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumConstantsTest {

    @Test
    void pulsarUrl_hasExpectedValue() {
        assertThat(EnumConstants.PULSAR_URL.getValue()).isEqualTo("pulsar://localhost:6650");
    }

    @Test
    void dateFormat_hasExpectedValue() {
        assertThat(EnumConstants.DATE_FORMAT.getValue()).isEqualTo("yyyy-MM-dd'T'HH:mm:ss");
    }

    @Test
    void defaultUuidString_isAllZeros() {
        assertThat(EnumConstants.DEFAULT_UUID_STRING.getValue()).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void standardHyphen_isHyphen() {
        assertThat(EnumConstants.STANDARD_HYPHEN.getValue()).isEqualTo("-");
    }

    @Test
    void standardUnderscore_isUnderscore() {
        assertThat(EnumConstants.STANDARD_UNDERSCORE.getValue()).isEqualTo("_");
    }
}
