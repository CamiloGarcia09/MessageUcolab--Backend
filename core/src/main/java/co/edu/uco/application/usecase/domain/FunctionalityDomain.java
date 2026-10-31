package co.edu.uco.application.usecase.domain;

import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

@Getter
public final class FunctionalityDomain {
    private UUID id;
    private String name;

    public FunctionalityDomain(UUID id, String name) {
        setId(id);
        setName(name);
    }

    public static FunctionalityDomain create(UUID id, String name) {
        return new FunctionalityDomain(id, name);
    }

    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }

    public void setName(String name) {
        this.name = trim(name);
    }

}
