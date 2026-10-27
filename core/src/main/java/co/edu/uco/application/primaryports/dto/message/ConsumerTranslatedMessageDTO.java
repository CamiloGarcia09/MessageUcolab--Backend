package co.edu.uco.application.primaryports.dto.message;

import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import lombok.Getter;

import java.time.Instant;

@Getter
public final class ConsumerTranslatedMessageDTO {
    private final String code;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final String title;
    private final String content;
    private final String type;
    private final String category;
    private final String functionality;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ConsumerTranslatedMessageDTO(ResolvedMessageDTO original, String sourceLanguage, String targetLanguage,
                                         MessageTranslationResponseData translation) {
        this.code = original.getCode();
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.title = translation.getTranslatedTitle();
        this.content = translation.getTranslatedContent();
        this.type = translation.getTranslatedType();
        this.category = translation.getTranslatedCategory();
        this.functionality = translation.getTranslatedFunctionality();
        this.createdAt = original.getCreatedAt();
        this.updatedAt = original.getUpdatedAt();
    }
}
