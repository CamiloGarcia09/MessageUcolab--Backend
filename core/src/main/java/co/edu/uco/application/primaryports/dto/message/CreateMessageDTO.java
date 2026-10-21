package co.edu.uco.application.primaryports.dto.message;

import co.edu.uco.crosscutting.helpers.UtilText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@Builder
@AllArgsConstructor
public final class CreateMessageDTO {

    private String code;
    private String title;
    private String content;
    private String typeId;
    private String categoryId;
    private String statusId;
    private String functionalityId;
    // Ambientes donde se crea el mismo mensaje (uno o varios de la aplicacion).
    private List<String> environmentIds;

    public CreateMessageDTO() {
        setCode(EMPTY);
        setTitle(EMPTY);
        setContent(EMPTY);
        setTypeId(EMPTY);
        setCategoryId(EMPTY);
        setStatusId(EMPTY);
        setFunctionalityId(EMPTY);
        setEnvironmentIds(List.of());
    }

    public void setCode(String code) { this.code = trim(code); }
    public void setTitle(String title) { this.title = trim(title); }
    public void setContent(String content) { this.content = trim(content); }
    public void setTypeId(String typeId) { this.typeId = trim(typeId); }
    public void setCategoryId(String categoryId) { this.categoryId = trim(categoryId); }
    public void setStatusId(String statusId) { this.statusId = trim(statusId); }
    public void setFunctionalityId(String functionalityId) { this.functionalityId = trim(functionalityId); }
    public void setEnvironmentIds(List<String> environmentIds) {
        this.environmentIds = environmentIds == null ? null : environmentIds.stream().map(UtilText::trim).toList();
    }
}
