package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
public final class TranslateMessageDTO {

    private String sourceLanguage;
    private String targetLanguage;

    public TranslateMessageDTO() {
        setSourceLanguage("auto");
        setTargetLanguage(EMPTY);
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = trim(sourceLanguage);
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = trim(targetLanguage);
    }
}
