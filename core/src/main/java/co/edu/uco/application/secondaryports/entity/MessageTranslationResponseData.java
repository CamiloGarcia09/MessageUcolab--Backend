package co.edu.uco.application.secondaryports.entity;

import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class MessageTranslationResponseData {
    private final String translatedTitle;
    private final String translatedContent;
    private final String translatedType;
    private final String translatedCategory;
    private final String translatedFunctionality;
    private final String provider;
    private final String model;
    private final long elapsedMillis;
    /** {@code false} cuando se entrega el texto original porque no hubo una traducción válida. */
    private final boolean translated;

    private MessageTranslationResponseData(
            String translatedTitle,
            String translatedContent,
            String translatedType,
            String translatedCategory,
            String translatedFunctionality,
            String provider,
            String model,
            long elapsedMillis,
            boolean translated
    ) {
        this.translatedTitle = trim(translatedTitle);
        this.translatedContent = trim(translatedContent);
        this.translatedType = trim(translatedType);
        this.translatedCategory = trim(translatedCategory);
        this.translatedFunctionality = trim(translatedFunctionality);
        this.provider = trim(provider);
        this.model = trim(model);
        this.elapsedMillis = elapsedMillis;
        this.translated = translated;
    }

    public static MessageTranslationResponseData create(
            String translatedTitle,
            String translatedContent,
            String translatedType,
            String translatedCategory,
            String translatedFunctionality,
            String provider,
            String model,
            long elapsedMillis
    ) {
        return new MessageTranslationResponseData(translatedTitle, translatedContent, translatedType,
                translatedCategory, translatedFunctionality, provider, model, elapsedMillis, true);
    }

    /**
     * Respaldo seguro: los textos originales, sin pasar por el modelo. Se usa cuando la traducción está
     * apagada, el modelo no responde o su respuesta no supera las validaciones.
     */
    public static MessageTranslationResponseData untranslated(
            String title,
            String content,
            String type,
            String category,
            String functionality
    ) {
        return new MessageTranslationResponseData(title, content, type, category, functionality,
                EMPTY, EMPTY, 0L, false);
    }
}
