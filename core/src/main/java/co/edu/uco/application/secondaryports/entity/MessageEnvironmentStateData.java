package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.crosscutting.helpers.UtilText;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

@Getter
public final class MessageEnvironmentStateData {
    private UUID id;
    private String name;
    public MessageEnvironmentStateData(UUID id, String name) {
        setId(id);
        setName(name);
    }
    public MessageEnvironmentStateData() {
        setId(UtilUUID.getNewUUID());
        setName(UtilText.EMPTY);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setName(String id) {
        this.name = trim(id);
    }
    public static MessageEnvironmentStateData build() {
        return new MessageEnvironmentStateData();
    }
}