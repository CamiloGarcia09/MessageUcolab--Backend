package co.edu.uco.messageucolab.client.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

record MessageResponse(
        String code,
        String sourceLanguage,
        String targetLanguage,
        String title,
        String content,
        String type,
        String category,
        String functionality,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
