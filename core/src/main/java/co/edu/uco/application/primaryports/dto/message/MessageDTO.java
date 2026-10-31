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
    private final String status;
    private final String statusId;
    private final String functionalityId;
    private final Instant updatedAt;

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
        this(code, title, content, type, category, functionality, createdAt, environments,
                "", null, null, null);
    }

    public MessageDTO(String code, String title, String content, String type, String category,
                      String functionality, Instant createdAt, List<CatalogItemDTO> environments,
                      String status, String statusId, String functionalityId, Instant updatedAt) {
        this.code = trim(code);
        this.title = trim(title);
        this.content = trim(content);
        this.type = trim(type);
        this.category = trim(category);
        this.functionality = trim(functionality);
        this.createdAt = createdAt;
        this.environments = environments == null ? List.of() : List.copyOf(environments);
        this.status = trim(status);
        this.statusId = statusId;
        this.functionalityId = functionalityId;
        this.updatedAt = updatedAt;
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
