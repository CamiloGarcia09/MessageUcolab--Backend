package co.edu.uco.application.common.catalog.strategy;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;

import java.util.List;
import java.util.Objects;

public record ConsumerTranslationSnapshot(ResolvedMessageDTO message, String sourceLanguage) {
    public List<String> values() {
        return List.of(message.getCode(), message.getTitle(), message.getContent(), message.getType(),
                message.getCategory(), message.getFunctionality(), Objects.toString(message.getCreatedAt(), ""),
                Objects.toString(message.getUpdatedAt(), ""), sourceLanguage);
    }
}
