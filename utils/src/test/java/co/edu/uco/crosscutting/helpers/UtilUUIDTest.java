package co.edu.uco.crosscutting.helpers;

import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilUUIDTest {

    private static final String DEFAULT_UUID_STRING = "00000000-0000-0000-0000-000000000000";

    @Test
    void defaultUuid_isAllZeros() {
        assertThat(UtilUUID.DEFAULT_UUID).hasToString(DEFAULT_UUID_STRING);
    }

    @Test
    void getDefaultUUID_returnsDefault_whenUuidIsNull() {
        assertThat(UtilUUID.getDefaultUUID(null)).isEqualTo(UtilUUID.DEFAULT_UUID);
    }

    @Test
    void getDefaultUUID_returnsSameUuid_whenNotNull() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.getDefaultUUID(uuid)).isEqualTo(uuid);
    }

    @Test
    void isEqual_returnsTrue_forEqualUuids() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.isEqual(uuid, uuid)).isTrue();
    }

    @Test
    void isEqual_returnsTrue_whenBothAreNull() {
        assertThat(UtilUUID.isEqual(null, null)).isTrue();
    }

    @Test
    void isEqual_returnsFalse_forDifferentUuids() {
        assertThat(UtilUUID.isEqual(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    void getNewUUID_returnsNonNullUuid() {
        assertThat(UtilUUID.getNewUUID()).isNotNull();
    }

    @Test
    void getNewUUID_returnsDifferentValuesOnEachCall() {
        UUID first = UtilUUID.getNewUUID();
        UUID second = UtilUUID.getNewUUID();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void getUUIDFromString_returnsDefaultUuid_whenStringIsNull() {
        assertThat(UtilUUID.getUUIDFromString(null)).isEqualTo(UtilUUID.DEFAULT_UUID);
    }

    @Test
    void getUUIDFromString_returnsDefaultUuid_whenStringIsBlank() {
        assertThat(UtilUUID.getUUIDFromString("   ")).isEqualTo(UtilUUID.DEFAULT_UUID);
    }

    @Test
    void getUUIDFromString_parsesValidUuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.getUUIDFromString(uuid.toString())).isEqualTo(uuid);
    }

    @Test
    void getUUIDFromString_throwsCrossWordsException_forInvalidUuid() {
        assertThatThrownBy(() -> UtilUUID.getUUIDFromString("not-a-uuid"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("El valor que se convertirá a UUID no tiene un formato válido."));
    }

    @Test
    void getStringFromUUID_returnsDefaultString_whenUuidIsNull() {
        assertThat(UtilUUID.getStringFromUUID(null)).isEqualTo(DEFAULT_UUID_STRING);
    }

    @Test
    void getStringFromUUID_returnsStringRepresentation() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.getStringFromUUID(uuid)).isEqualTo(uuid.toString());
    }

    @Test
    void getStringToUUID_delegatesToGetUUIDFromString() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.getStringToUUID(uuid.toString())).isEqualTo(uuid);
    }

    @Test
    void isNull_returnsTrue_whenUuidIsNull() {
        assertThat(UtilUUID.isNull(null)).isTrue();
    }

    @Test
    void isNull_returnsFalse_whenUuidIsNotNull() {
        assertThat(UtilUUID.isNull(UUID.randomUUID())).isFalse();
    }

    @Test
    void formatUUID_replacesHyphensWithUnderscores() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertThat(UtilUUID.formatUUID(uuid)).isEqualTo("123e4567_e89b_12d3_a456_426614174000");
    }

    @Test
    void formatUUID_throwsNullPointerException_whenUuidIsNull() {
        assertThatThrownBy(() -> UtilUUID.formatUUID(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unformatUUID_replacesUnderscoresWithHyphens() {
        UUID result = UtilUUID.unformatUUID("123e4567_e89b_12d3_a456_426614174000");
        assertThat(result.toString()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void unformatUUID_throwsIllegalArgumentException_forInvalidUuid() {
        assertThatThrownBy(() -> UtilUUID.unformatUUID("invalid_value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatAndUnformat_areInverseOperations() {
        UUID uuid = UUID.randomUUID();
        assertThat(UtilUUID.unformatUUID(UtilUUID.formatUUID(uuid))).isEqualTo(uuid);
    }
}
