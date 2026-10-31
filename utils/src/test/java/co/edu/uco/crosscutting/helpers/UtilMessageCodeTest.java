package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static co.edu.uco.crosscutting.helpers.UtilMessageCode.MAX_SUFFIX_LENGTH;
import static co.edu.uco.crosscutting.helpers.UtilMessageCode.SUFFIX_PATTERN;
import static co.edu.uco.crosscutting.helpers.UtilMessageCode.normalize;
import static org.assertj.core.api.Assertions.assertThat;

class UtilMessageCodeTest {

    @Test
    void normalize_prependsMsgPrefix_whenSuffixIsAlreadyUppercase() {
        assertThat(normalize("WELCOME")).isEqualTo("MSG_WELCOME");
    }

    @Test
    void normalize_uppercasesSuffix_whenSuffixUsesLowerCaseLetters() {
        assertThat(normalize("welcome")).isEqualTo("MSG_WELCOME");
    }

    @Test
    void normalize_collapsesConsecutiveSpacesIntoSingleUnderscore() {
        assertThat(normalize("welcome  message")).isEqualTo("MSG_WELCOME_MESSAGE");
    }

    @Test
    void normalize_replacesTabsAndNewlinesWithUnderscore() {
        assertThat(normalize("welcome\tmessage\nagain"))
                .isEqualTo("MSG_WELCOME_MESSAGE_AGAIN");
    }

    @Test
    void normalize_trimsSurroundingWhitespaceBeforePrefixing() {
        assertThat(normalize("  welcome  ")).isEqualTo("MSG_WELCOME");
    }

    @ParameterizedTest(name = "sufijo \"{0}\" -> \"MSG_\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void normalize_returnsOnlyPrefix_whenSuffixIsNullOrBlank(String suffix) {
        assertThat(normalize(suffix)).isEqualTo("MSG_");
    }

    @Test
    void normalize_doesNotTruncateSuffix_whenSuffixExceedsMaximumLength() {
        String suffix = "A".repeat(MAX_SUFFIX_LENGTH + 5);

        assertThat(normalize(suffix)).isEqualTo("MSG_" + suffix);
    }

    @Test
    void normalize_usesRootLocale_whenDefaultLocaleUppercasesDottedI() {
        Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(normalize("i")).isEqualTo("MSG_I");
        } finally {
            Locale.setDefault(previousDefault);
        }
    }

    @ParameterizedTest(name = "sufijo \"{0}\" -> formato válido: {1}")
    @CsvSource({
            "WELCOME, true",
            "WELCOME 1, true",
            "abc123, true",
            "MSG-001, false",
            "HELLO_WORLD, false",
            "HOLA.MUNDO, false",
            "NUMEROS#2, false"
    })
    void suffixPattern_acceptsOnlyLettersDigitsAndSpaces(String suffix, boolean expectedMatch) {
        assertThat(suffix.matches(SUFFIX_PATTERN))
                .as("SUFFIX_PATTERN debe validar \"%s\" como %s", suffix, expectedMatch)
                .isEqualTo(expectedMatch);
    }
}
