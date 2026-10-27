package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class ConsumerTranslationRequestDTO {
    private String targetLanguage;

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = trim(targetLanguage);
    }
}
