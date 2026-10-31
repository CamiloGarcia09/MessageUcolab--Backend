package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilTextTest {

    @Test
    void isNull_returnsTrue_whenValueIsNull() {
        assertThat(UtilText.isNull(null)).isTrue();
    }

    @Test
    void isNull_returnsFalse_whenValueIsNotNull() {
        assertThat(UtilText.isNull("text")).isFalse();
    }

    @Test
    void getDefault_returnsDefault_whenValueIsNull() {
        assertThat(UtilText.getDefault(null, "fallback")).isEqualTo("fallback");
    }

    @Test
    void getDefault_returnsValue_whenValueIsNotNull() {
        assertThat(UtilText.getDefault("value", "fallback")).isEqualTo("value");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getDefault_returnsEmpty_whenValueIsNullOrEmpty(String value) {
        assertThat(UtilText.getDefault(value)).isEmpty();
    }

    @Test
    void trim_returnsEmptyString_whenValueIsNull() {
        assertThat(UtilText.trim(null)).isEmpty();
    }

    @Test
    void trim_removesLeadingAndTrailingSpaces() {
        assertThat(UtilText.trim("  hello world  ")).isEqualTo("hello world");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void isEmpty_returnsTrue_forNullOrBlankValues(String value) {
        assertThat(UtilText.isEmpty(value)).isTrue();
    }

    @Test
    void isEmpty_returnsFalse_forNonEmptyValue() {
        assertThat(UtilText.isEmpty("text")).isFalse();
    }

    @Test
    void isEmptyOrNull_returnsTrue_whenValueIsNull() {
        assertThat(UtilText.isEmptyOrNull(null)).isTrue();
    }

    @Test
    void isEmptyOrNull_returnsTrue_whenValueIsBlank() {
        assertThat(UtilText.isEmptyOrNull("   ")).isTrue();
    }

    @Test
    void isEmptyOrNull_returnsFalse_whenValueHasContent() {
        assertThat(UtilText.isEmptyOrNull("content")).isFalse();
    }

    @Test
    void isContainsOnlyLetterAndSpace_returnsTrue_forLettersAndSpaces() {
        assertThat(UtilText.isContainsOnlyLetterAndSpace("hola mundo")).isTrue();
    }

    @Test
    void isContainsOnlyLetterAndSpace_returnsTrue_forSpanishCharacters() {
        assertThat(UtilText.isContainsOnlyLetterAndSpace("niño Ñandu")).isTrue();
    }

    @Test
    void isContainsOnlyLetterAndSpace_returnsFalse_whenContainsNumbers() {
        assertThat(UtilText.isContainsOnlyLetterAndSpace("hola 123")).isFalse();
    }

    @Test
    void isContainsOnlyLetterAndSpace_returnsFalse_whenContainsSymbols() {
        assertThat(UtilText.isContainsOnlyLetterAndSpace("hola@mundo")).isFalse();
    }

    @Test
    void isContainsOnlyLetterAndSpace_throwsNullPointerException_whenValueIsNull() {
        assertThrows(NullPointerException.class, () -> UtilText.isContainsOnlyLetterAndSpace(null));
    }

    @Test
    void validMatch_returnsTrue_whenMatchesExpression() {
        assertThat(UtilText.validMatch("12345", UtilText.ONLY_NUMBERS)).isTrue();
    }

    @Test
    void validMatch_returnsFalse_whenDoesNotMatchExpression() {
        assertThat(UtilText.validMatch("abc", UtilText.ONLY_NUMBERS)).isFalse();
    }

    @Test
    void validMatch_throwsNullPointerException_whenValueIsNull() {
        assertThrows(NullPointerException.class, () -> UtilText.validMatch(null, UtilText.ONLY_NUMBERS));
    }

    @Test
    void concatenateWithoutSeparator_joinsValues() {
        assertThat(UtilText.concatenateWithoutSeparator("a", "b", "c")).isEqualTo("abc");
    }

    @Test
    void concatenateWithoutSeparator_returnsEmpty_whenNoValues() {
        assertThat(UtilText.concatenateWithoutSeparator()).isEmpty();
    }

    @Test
    void concatenateWithSeparator_joinsValuesWithSeparator() {
        assertThat(UtilText.concatenateWithSeparator("-", "a", "b")).isEqualTo("a-b");
    }

    @Test
    void concatenateWithSeparator_returnsEmpty_whenNoValues() {
        assertThat(UtilText.concatenateWithSeparator("-")).isEmpty();
    }

    @Test
    void stringToUpperCase_returnsEmpty_whenValueIsNull() {
        assertThat(UtilText.stringToUpperCase(null)).isEmpty();
    }

    @Test
    void stringToUpperCase_convertsToUpperCase() {
        assertThat(UtilText.stringToUpperCase("hello")).isEqualTo("HELLO");
    }
}
