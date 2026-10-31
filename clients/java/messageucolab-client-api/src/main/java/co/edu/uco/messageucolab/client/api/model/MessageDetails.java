package co.edu.uco.messageucolab.client.api.model;

import java.time.Instant;

public record MessageDetails(
        String code,
        String sourceLanguage,
        String targetLanguage,
        String title,
        String content,
        String type,
        String category,
        String functionality,
        Instant createdAt,
        Instant updatedAt
) {
}
