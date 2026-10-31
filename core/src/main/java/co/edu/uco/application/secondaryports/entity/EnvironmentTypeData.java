package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.crosscutting.helpers.UtilText;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

@Getter
public final class EnvironmentTypeData {
    private UUID id;
    private String name;
    public EnvironmentTypeData() {
        setId(UtilUUID.getNewUUID());
        setName(UtilText.EMPTY);
    }
    public EnvironmentTypeData(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setName(String name) {
        this.name = trim(name);
    }
    public static EnvironmentTypeData build() {
        return new EnvironmentTypeData();
    }
}