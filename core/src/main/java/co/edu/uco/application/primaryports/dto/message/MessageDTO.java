package co.edu.uco.application.primaryports.dto.message;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

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
    private final String functionality;
    private final Instant createdAt;
    private final List<CatalogItemDTO> environments;

    public MessageDTO(String code, String title, String content, String type, String category,
                      String functionality) {
        this(code, title, content, type, category, functionality, null);
    }

    public MessageDTO(String code, String title, String content, String type, String category,
                      String functionality, Instant createdAt) {
        this(code, title, content, type, category, functionality, createdAt, List.of());
    }
    public MessageDTO(String code, String title, String content, String type, String category,
                      String functionality, Instant createdAt, List<CatalogItemDTO> environments) {
        this.code = trim(code);
        this.title = trim(title);
        this.content = trim(content);
        this.type = trim(type);
        this.category = trim(category);
        this.functionality = trim(functionality);
        this.createdAt = createdAt;
        this.environments = environments == null ? List.of() : List.copyOf(environments);
    }

    public static MessageDTO create(String code, String title, String content, String type, String category,
                                    String functionality) {
        return new MessageDTO(code, title, content, type, category, functionality);
    }

    public static MessageDTO create(String code, String title, String content, String type, String category,
                                    String functionality, Instant createdAt) {
        return new MessageDTO(code, title, content, type, category, functionality, createdAt);
    }
}
