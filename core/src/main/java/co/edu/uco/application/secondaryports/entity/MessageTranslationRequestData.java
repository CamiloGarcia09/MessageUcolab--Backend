package co.edu.uco.application.secondaryports.entity;

import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class MessageTranslationRequestData {
    private final String code;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final String title;
    private final String content;
    private final String type;
    private final String category;
    private final String application;
    private final String functionality;

    private MessageTranslationRequestData(
            String code,
            String sourceLanguage,
            String targetLanguage,
            String title,
            String content,
            String type,
            String category,
            String application,
            String functionality
    ) {
        this.code = trim(code);
        this.sourceLanguage = trim(sourceLanguage);
        this.targetLanguage = trim(targetLanguage);
        this.title = trim(title);
        this.content = trim(content);
        this.type = trim(type);
        this.category = trim(category);
        this.application = trim(application);
        this.functionality = trim(functionality);
    }

    public static MessageTranslationRequestData create(
            String code,
            String sourceLanguage,
            String targetLanguage,
            String title,
            String content,
            String type,
            String category,
            String application,
            String functionality
    ) {
        return new MessageTranslationRequestData(
                code,
                sourceLanguage,
                targetLanguage,
                title,
                content,
                type,
                category,
                application,
                functionality
        );
    }
}
