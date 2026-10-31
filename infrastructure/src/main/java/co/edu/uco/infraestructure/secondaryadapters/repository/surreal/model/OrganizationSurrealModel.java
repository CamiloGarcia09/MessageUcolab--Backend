package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model;

import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

@Getter
public final class OrganizationSurrealModel {

    private UUID id;
    private String name;

    public OrganizationSurrealModel(final UUID id, final String name) {
        setId(id);
        setName(name);
    }

    public OrganizationSurrealModel() {
        setId(getNewUUID());
        setName(EMPTY);
    }

    public void setId(final UUID id) {
        this.id = getDefaultUUID(id);
    }

    public void setName(final String name) {
        this.name = trim(name);
    }

    public static OrganizationSurrealModel build() {
        return new OrganizationSurrealModel();
    }
}
