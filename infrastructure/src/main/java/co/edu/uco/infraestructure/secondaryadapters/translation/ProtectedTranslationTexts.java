package co.edu.uco.infraestructure.secondaryadapters.translation;

import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepara los textos que van al modelo y valida lo que devuelve, con reglas deterministas que no dependen
 * de que el modelo obedezca (OWASP LLM01 y LLM10):
 * <ul>
 *     <li>quita caracteres invisibles y de control, que pueden esconder instrucciones;</li>
 *     <li>cambia marcadores ({@code {nombre}}, {@code %s}, {@code ${valor}}), enlaces, etiquetas y
 *     entidades HTML por fichas {@code [[T0]]}, {@code [[T1]]}… para que el modelo no pueda alterarlos;</li>
 *     <li>exige que la respuesta conserve exactamente esas fichas, que no traiga enlaces ni HTML nuevos,
 *     que su longitud sea razonable y que de verdad esté traducida.</li>
 * </ul>
 * Si algo no se cumple lanza {@link IllegalStateException} con un motivo técnico que nunca incluye el
 * texto del mensaje.
 */
final class ProtectedTranslationTexts {

    static final String INVALID_MARKERS = "La traducción no conserva los marcadores protegidos";
    static final String UNEXPECTED_MARKUP = "La traducción agrega enlaces, HTML o marcadores que no existen en el original";
    static final String UNEXPECTED_LENGTH = "La longitud de la traducción no corresponde con la del original";
    static final String NOT_TRANSLATED = "El modelo devolvió el texto sin traducir";
    static final String MARKER_COLLISION = "El texto original ya contiene fichas con forma de marcador protegido";

    private static final Pattern INVISIBLE = Pattern.compile(
            "[\\u00AD\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\u2066-\\u2069\\uFE00-\\uFE0F\\uFEFF"
                    + "\\x{E0000}-\\x{E007F}]");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\n\\r\\t]]");
    private static final Pattern PROTECTED = Pattern.compile(
            "(?:https?://|www\\.)[^\\s\"'<>]+"
                    + "|</?[A-Za-z][^<>]{0,300}>"
                    + "|&(?:[A-Za-z][A-Za-z0-9]{1,30}|#\\d{1,7}|#[xX][0-9A-Fa-f]{1,6});"
                    + "|\\$\\{[^{}]{1,80}\\}"
                    + "|\\{[^{}\\s]{0,80}\\}"
                    + "|%(?:\\d+\\$)?[sd](?![A-Za-z])"
                    + "|%%");
    private static final Pattern TOKEN = Pattern.compile("\\[\\[T(\\d+)]]");
    private static final int MIN_LENGTH_TO_COMPARE = 30;
    private static final double MIN_LENGTH_RATIO = 0.2;
    private static final double MAX_LENGTH_RATIO = 4.0;

    private final List<String> originals = new ArrayList<>();
    private final boolean sameLanguage;
    private final String title;
    private final String content;
    private final String type;
    private final String category;
    private final String functionality;

    ProtectedTranslationTexts(MessageTranslationRequestData request) {
        this.sameLanguage = primaryLanguage(request.getSourceLanguage())
                .equals(primaryLanguage(request.getTargetLanguage()));
        this.title = protect(request.getTitle());
        this.content = protect(request.getContent());
        this.type = protect(request.getType());
        this.category = protect(request.getCategory());
        this.functionality = protect(request.getFunctionality());
    }

    String title() {
        return title;
    }

    String content() {
        return content;
    }

    String type() {
        return type;
    }

    String category() {
        return category;
    }

    String functionality() {
        return functionality;
    }

    String restoreTitle(String translated) {
        return restore(title, translated, true);
    }

    String restoreContent(String translated) {
        return restore(content, translated, true);
    }

    String restoreType(String translated) {
        return restore(type, translated, false);
    }

    String restoreCategory(String translated) {
        return restore(category, translated, false);
    }

    String restoreFunctionality(String translated) {
        return restore(functionality, translated, false);
    }

    private String protect(String text) {
        var clean = sanitize(text);
        if (TOKEN.matcher(clean).find()) {
            throw new IllegalStateException(MARKER_COLLISION);
        }
        var matcher = PROTECTED.matcher(clean);
        var protectedText = new StringBuilder();
        while (matcher.find()) {
            originals.add(matcher.group());
            matcher.appendReplacement(protectedText, Matcher.quoteReplacement("[[T" + (originals.size() - 1) + "]]"));
        }
        matcher.appendTail(protectedText);
        return protectedText.toString();
    }

    private String restore(String source, String translated, boolean prose) {
        var clean = sanitize(translated);
        if (!tokens(source).equals(tokens(clean))) {
            throw new IllegalStateException(INVALID_MARKERS);
        }
        var translatedText = TOKEN.matcher(clean).replaceAll("");
        if (PROTECTED.matcher(translatedText).find()) {
            throw new IllegalStateException(UNEXPECTED_MARKUP);
        }
        if (prose) {
            validateProse(TOKEN.matcher(source).replaceAll("").strip(), translatedText.strip());
        }
        var matcher = TOKEN.matcher(clean);
        var restored = new StringBuilder();
        while (matcher.find()) {
            var original = originals.get(Integer.parseInt(matcher.group(1)));
            matcher.appendReplacement(restored, Matcher.quoteReplacement(original));
        }
        matcher.appendTail(restored);
        return restored.toString();
    }

    private void validateProse(String original, String translated) {
        if (original.length() < MIN_LENGTH_TO_COMPARE) {
            return;
        }
        var ratio = (double) translated.length() / original.length();
        if (ratio < MIN_LENGTH_RATIO || ratio > MAX_LENGTH_RATIO) {
            throw new IllegalStateException(UNEXPECTED_LENGTH);
        }
        if (!sameLanguage && normalize(original).equals(normalize(translated))) {
            throw new IllegalStateException(NOT_TRANSLATED);
        }
    }

    private static List<String> tokens(String text) {
        var found = new ArrayList<String>();
        var matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        found.sort(null);
        return found;
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return CONTROL.matcher(INVISIBLE.matcher(text).replaceAll("")).replaceAll("");
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String primaryLanguage(String language) {
        var value = language == null ? "" : language.strip().toLowerCase(Locale.ROOT);
        return value.split("[-_\\s]", 2)[0];
    }
}
