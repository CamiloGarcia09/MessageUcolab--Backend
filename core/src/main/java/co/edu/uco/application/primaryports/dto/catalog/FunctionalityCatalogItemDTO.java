package co.edu.uco.application.primaryports.dto.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class FunctionalityCatalogItemDTO extends CatalogItemDTO {

    private final String stateId;
    private final String state;
    private final boolean active;

    public FunctionalityCatalogItemDTO(String id, String name, String stateId, String state, boolean active) {
        super(id, name);
        this.stateId = stateId;
        this.state = trim(state);
        this.active = active;
    }
}
