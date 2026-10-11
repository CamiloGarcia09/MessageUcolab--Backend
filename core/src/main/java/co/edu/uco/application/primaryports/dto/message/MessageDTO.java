package co.edu.uco.application.primaryports.dto.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@EqualsAndHashCode
@ToString
public final class MessageDTO {

    private final String code;
    private final String title;
    private final String content;
    private final String type;
    private final String category;
    private final String application;
    private final String functionality;
    private final Instant createdAt;

    public MessageDTO(String code, String title, String content, String type, String category, String application,
                      String functionality) {
        this(code, title, content, type, category, application, functionality, null);
    }

    public MessageDTO(String code, String title, String content, String type, String category, String application,
                      String functionality, Instant createdAt) {
        this.code = trim(code);
        this.title = trim(title);
        this.content = trim(content);
        this.application = trim(application);
        this.type = trim(type);
        this.category = trim(category);
        this.functionality = trim(functionality);
        this.createdAt = createdAt;
    }

    public static MessageDTO create(String code, String title, String content, String type, String category,
                                    String application, String functionality) {
        return new MessageDTO(code, title, content, type, category, application, functionality);
    }

    public static MessageDTO create(String code, String title, String content, String type, String category,
                                    String application, String functionality, Instant createdAt) {
        return new MessageDTO(code, title, content, type, category, application, functionality, createdAt);
    }
}
