package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@ToString
public final class ResolvedMessageDTO {

    private final String code;
    private final String title;
    private final String content;
    private final String type;
    private final String category;
    private final String functionality;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ResolvedMessageDTO(String code, String title, String content) {
        this(code, title, content, "", "", "", null, null);
    }

    public ResolvedMessageDTO(String code, String title, String content, String type,
                              String category, String functionality, Instant createdAt, Instant updatedAt) {
        this.code = trim(code);
        this.title = trim(title);
        this.content = trim(content);
        this.type = trim(type);
        this.category = trim(category);
        this.functionality = trim(functionality);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
