package co.edu.uco.application.primaryports.dto.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@Builder
@AllArgsConstructor
public final class CreateApplicationDTO {

    // Compatibilidad de entrada: el servidor siempre genera un UUID propio.
    private String id;
    private String name;
    private String organizationId;
    private String languageId;
    private String stateId;

    public CreateApplicationDTO() {
        setId(EMPTY);
        setName(EMPTY);
        setOrganizationId(EMPTY);
        setLanguageId(EMPTY);
        setStateId(EMPTY);
    }

    public void setId(String id) { this.id = trim(id); }
    public void setName(String name) { this.name = trim(name); }
    public void setOrganizationId(String organizationId) { this.organizationId = trim(organizationId); }
    public void setLanguageId(String languageId) { this.languageId = trim(languageId); }
    public void setStateId(String stateId) { this.stateId = trim(stateId); }
}
