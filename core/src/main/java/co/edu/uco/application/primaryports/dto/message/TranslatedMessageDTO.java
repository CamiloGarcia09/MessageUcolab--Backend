package co.edu.uco.application.primaryports.dto.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@EqualsAndHashCode
@ToString
public final class TranslatedMessageDTO {

    private final String code;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final String originalTitle;
    private final String originalContent;
    private final String translatedTitle;
    private final String translatedContent;
    private final String originalType;
    private final String translatedType;
    private final String originalCategory;
    private final String translatedCategory;
    private final String originalFunctionality;
    private final String translatedFunctionality;
    private final String translationProvider;
    private final String translationModel;
    private final long translationElapsedMs;
    private final boolean dynamicTranslation;

    public TranslatedMessageDTO(
            String code,
            String sourceLanguage,
            String targetLanguage,
            String originalTitle,
            String originalContent,
            String translatedTitle,
            String translatedContent,
            String originalType,
            String translatedType,
            String originalCategory,
            String translatedCategory,
            String originalFunctionality,
            String translatedFunctionality,
            String translationProvider,
            String translationModel,
            long translationElapsedMs,
            boolean dynamicTranslation
    ) {
        this.code = trim(code);
        this.sourceLanguage = trim(sourceLanguage);
        this.targetLanguage = trim(targetLanguage);
        this.originalTitle = trim(originalTitle);
        this.originalContent = trim(originalContent);
        this.translatedTitle = trim(translatedTitle);
        this.translatedContent = trim(translatedContent);
        this.originalType = trim(originalType);
        this.translatedType = trim(translatedType);
        this.originalCategory = trim(originalCategory);
        this.translatedCategory = trim(translatedCategory);
        this.originalFunctionality = trim(originalFunctionality);
        this.translatedFunctionality = trim(translatedFunctionality);
        this.translationProvider = trim(translationProvider);
        this.translationModel = trim(translationModel);
        this.translationElapsedMs = translationElapsedMs;
        this.dynamicTranslation = dynamicTranslation;
    }

    public static TranslatedMessageDTO create(
            String code,
            String sourceLanguage,
            String targetLanguage,
            String originalTitle,
            String originalContent,
            String translatedTitle,
            String translatedContent,
            String originalType,
            String translatedType,
            String originalCategory,
            String translatedCategory,
            String originalFunctionality,
            String translatedFunctionality,
            String translationProvider,
            String translationModel,
            long translationElapsedMs
    ) {
        return new TranslatedMessageDTO(
                code,
                sourceLanguage,
                targetLanguage,
                originalTitle,
                originalContent,
                translatedTitle,
                translatedContent,
                originalType,
                translatedType,
                originalCategory,
                translatedCategory,
                originalFunctionality,
                translatedFunctionality,
                translationProvider,
                translationModel,
                translationElapsedMs,
                true
        );
    }
}
