package co.edu.uco.application.primaryports.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@Builder
@AllArgsConstructor
public final class CreateOrganizationDTO {

    private String name;

    public CreateOrganizationDTO() {
        setName(EMPTY);
    }

    public void setName(final String name) {
        this.name = trim(name);
    }
}
