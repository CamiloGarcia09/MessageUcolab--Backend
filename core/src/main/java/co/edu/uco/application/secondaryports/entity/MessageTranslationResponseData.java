package co.edu.uco.application.secondaryports.entity;

import lombok.Getter;

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

    private MessageTranslationResponseData(
            String translatedTitle,
            String translatedContent,
            String translatedType,
            String translatedCategory,
            String translatedFunctionality,
            String provider,
            String model,
            long elapsedMillis
    ) {
        this.translatedTitle = trim(translatedTitle);
        this.translatedContent = trim(translatedContent);
        this.translatedType = trim(translatedType);
        this.translatedCategory = trim(translatedCategory);
        this.translatedFunctionality = trim(translatedFunctionality);
        this.provider = trim(provider);
        this.model = trim(model);
        this.elapsedMillis = elapsedMillis;
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
                translatedCategory, translatedFunctionality, provider, model, elapsedMillis);
    }
}
