package co.edu.uco.application.secondaryports.entity;

import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

@Getter
public final class StatusMessageData {
    private UUID id;
    private String name;

    public StatusMessageData() {
        setId(getNewUUID());
        setName(EMPTY);
    }
    public StatusMessageData(UUID id, String name) {
        setId(id);
        setName(name);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setName(String name) {
        this.name = trim(name);
    }
    public static StatusMessageData build() {
        return new StatusMessageData();
    }
}