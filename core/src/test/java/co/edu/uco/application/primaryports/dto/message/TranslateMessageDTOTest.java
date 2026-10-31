package co.edu.uco.application.primaryports.dto.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslateMessageDTOTest {

    @Test
    void defaultConstructor_setsAutomaticSourceLanguageAndEmptyTargetLanguage() {
        TranslateMessageDTO dto = new TranslateMessageDTO();

        assertAll(
                () -> assertEquals("auto", dto.getSourceLanguage()),
                () -> assertEquals("", dto.getTargetLanguage()));
    }

    @Test
    void setters_trimValues() {
        TranslateMessageDTO dto = new TranslateMessageDTO();
        dto.setSourceLanguage("  es  ");
        dto.setTargetLanguage("  en  ");

        assertAll(
                () -> assertEquals("es", dto.getSourceLanguage()),
                () -> assertEquals("en", dto.getTargetLanguage()));
    }

    @Test
    void setters_normalizeNullToEmpty() {
        TranslateMessageDTO dto = new TranslateMessageDTO();
        dto.setSourceLanguage(null);
        dto.setTargetLanguage(null);

        assertAll(
                () -> assertEquals("", dto.getSourceLanguage()),
                () -> assertEquals("", dto.getTargetLanguage()));
    }
}
