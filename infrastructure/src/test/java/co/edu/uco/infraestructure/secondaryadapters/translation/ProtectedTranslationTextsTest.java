package co.edu.uco.infraestructure.secondaryadapters.translation;

import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtectedTranslationTextsTest {

    private static final String LONG_TEXT = "Este es un texto largo que se traducirá al inglés sin perder el sentido.";

    private static MessageTranslationRequestData request(String sourceLanguage, String targetLanguage, String title,
                                                         String content) {
        return MessageTranslationRequestData.create("MSG_PRUEBA", sourceLanguage, targetLanguage, title, content,
                "Funcional", "Información", "Inventario", "Gestión");
    }

    @Test
    void protects_replacesPlaceholdersLinksAndMarkupWithTokens() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Hola {nombre}",
                "Visita https://ejemplo.com o <b>escribe</b> %s y ${valor}"));

        assertThat(texts.title()).isEqualTo("Hola [[T0]]");
        assertThat(texts.content()).doesNotContain("{nombre}", "https://ejemplo.com", "<b>", "${valor}", "%s");
        assertThat(texts.content()).contains("[[T1]]", "[[T2]]");
    }

    @Test
    void restore_putsTheOriginalValuesBackInTheTranslation() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Hola {nombre}", "Visita https://ejemplo.com ya"));

        assertThat(texts.restoreTitle("Hello [[T0]]")).isEqualTo("Hello {nombre}");
        assertThat(texts.restoreContent("Visit [[T1]] now")).isEqualTo("Visit https://ejemplo.com now");
    }

    @Test
    void sanitize_removesInvisibleAndControlCharactersThatCanHideInstructions() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Ho" + (char) 0x200B + "la" + (char) 7,
                "Texto" + (char) 0x202E + "oculto"));

        assertThat(texts.title()).isEqualTo("Hola");
        assertThat(texts.content()).isEqualTo("Textooculto");
    }

    @Test
    void protect_rejectsAnOriginalTextThatAlreadyLooksLikeAProtectedToken() {
        assertThatThrownBy(() -> new ProtectedTranslationTexts(request("es", "en", "Hola [[T0]]", "Contenido")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.MARKER_COLLISION);
    }

    @Test
    void restore_rejectsATranslationThatDropsAProtectedToken() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Hola {nombre}", "Contenido"));

        assertThatThrownBy(() -> texts.restoreTitle("Hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.INVALID_MARKERS);
    }

    @Test
    void restore_rejectsATranslationThatInventsATokenOrRepeatsOne() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Hola {nombre}", "Contenido"));

        assertThatThrownBy(() -> texts.restoreTitle("Hello [[T0]] [[T0]]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.INVALID_MARKERS);
        assertThatThrownBy(() -> texts.restoreTitle("Hello [[T0]] [[T7]]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.INVALID_MARKERS);
    }

    @Test
    void restore_rejectsATranslationThatAddsLinksOrHtml() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Hola", "Contenido"));

        assertThatThrownBy(() -> texts.restoreContent("Content https://malicioso.example"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.UNEXPECTED_MARKUP);
        assertThatThrownBy(() -> texts.restoreContent("Content <script>alert(1)</script>"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.UNEXPECTED_MARKUP);
    }

    @Test
    void restore_rejectsAnAbsurdLengthForLongTexts() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Título", LONG_TEXT));

        assertThatThrownBy(() -> texts.restoreContent("Ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.UNEXPECTED_LENGTH);
        assertThatThrownBy(() -> texts.restoreContent(LONG_TEXT.repeat(6)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.UNEXPECTED_LENGTH);
    }

    @Test
    void restore_rejectsATextReturnedUntranslatedWhenLanguagesDiffer() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Título", LONG_TEXT));

        assertThatThrownBy(() -> texts.restoreContent("  " + LONG_TEXT.toUpperCase() + " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProtectedTranslationTexts.NOT_TRANSLATED);
    }

    @Test
    void restore_acceptsTheSameTextWhenSourceAndTargetShareTheLanguage() {
        var texts = new ProtectedTranslationTexts(request("es", "es-419", "Título", LONG_TEXT));

        assertThatCode(() -> texts.restoreContent(LONG_TEXT)).doesNotThrowAnyException();
    }

    @Test
    void restore_acceptsAValidTranslation() {
        var texts = new ProtectedTranslationTexts(request("es", "en", "Producto {nombre}", LONG_TEXT));

        assertThat(texts.restoreTitle("Product [[T0]]")).isEqualTo("Product {nombre}");
        assertThat(texts.restoreContent("This is a long text that will be translated into English keeping its meaning."))
                .startsWith("This is a long text");
        assertThat(texts.restoreType("Functional")).isEqualTo("Functional");
    }
}
