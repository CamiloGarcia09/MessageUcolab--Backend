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
public final class UpdateMessageDTO {

    private String title;
    private String content;
    private String typeId;
    private String categoryId;
    private String statusId;
    private String functionalityId;
    // Opcional: si se envia, reemplaza los ambientes del mensaje; si es nulo no los cambia.
    private List<String> environmentIds;

    public UpdateMessageDTO() {
        setTitle(EMPTY);
        setContent(EMPTY);
        setTypeId(EMPTY);
        setCategoryId(EMPTY);
        setStatusId(EMPTY);
        setFunctionalityId(EMPTY);
    }

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
