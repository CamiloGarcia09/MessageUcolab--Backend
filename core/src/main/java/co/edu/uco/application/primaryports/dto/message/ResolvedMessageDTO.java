package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;
import lombok.ToString;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@ToString
public final class ResolvedMessageDTO {

    private final String code;
    private final String title;
    private final String content;

    public ResolvedMessageDTO(String code, String title, String content) {
        this.code = trim(code);
        this.title = trim(title);
        this.content = trim(content);
    }
}
