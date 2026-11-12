package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

/**
 * Acepta solo etiquetas de idioma reales antes de pedir una traducción al modelo: un idioma ISO 639,
 * con escritura y región opcionales (por ejemplo {@code es}, {@code pt-br}, {@code zh-hans} o
 * {@code es-419}). Así una etiqueta inventada, como {@code fr-aa1}, no provoca una llamada nueva al
 * modelo en la ruta pública (OWASP LLM06, consumo sin límite).
 */
public final class ConsumerLanguageCodeKnownRule extends RuleValidator<String> {

    private static final Set<String> LANGUAGES = Stream.concat(
                    Arrays.stream(Locale.getISOLanguages()),
                    Arrays.stream(Locale.getISOLanguages()).map(ConsumerLanguageCodeKnownRule::iso3Language))
            .filter(language -> !isEmptyOrNull(language))
            .map(language -> language.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> REGIONS = Stream.concat(
                    Arrays.stream(Locale.getISOCountries()).map(region -> region.toLowerCase(Locale.ROOT)),
                    Stream.of("419"))
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> SCRIPTS = Set.of("latn", "cyrl", "arab", "hans", "hant", "deva", "grek", "hebr",
            "jpan", "kore", "thai");

    public ConsumerLanguageCodeKnownRule(CatalogPort catalog, MessageCatalogCodeEnum code) {
        super(catalog, ConsumerLanguageCodeKnownRule::isKnown, code);
    }

    private static boolean isKnown(String languageTag) {
        if (isEmptyOrNull(languageTag)) {
            return false;
        }
        var parts = trim(languageTag).replace('_', '-').toLowerCase(Locale.ROOT).split("-", -1);
        if (!LANGUAGES.contains(parts[0]) || parts.length > 3) {
            return false;
        }
        var index = 1;
        if (index < parts.length && SCRIPTS.contains(parts[index])) {
            index++;
        }
        if (index < parts.length && REGIONS.contains(parts[index])) {
            index++;
        }
        return index == parts.length;
    }

    private static String iso3Language(String language) {
        try {
            return Locale.of(language).getISO3Language();
        } catch (java.util.MissingResourceException exception) {
            return null;
        }
    }
}
