package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

/**
 * Ambiente en el que existe un mensaje. Solo conserva id y nombre para que el mensaje
 * pueda serializarse en cache sin referencias circulares.
 */
@Getter
public final class MessageEnvironmentRefData {
    private UUID id;
    private String name;

    public MessageEnvironmentRefData() {
        setId(UtilUUID.DEFAULT_UUID);
        setName(EMPTY);
    }

    public MessageEnvironmentRefData(UUID id, String name) {
        setId(id);
        setName(name);
    }

    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }

    public void setName(String name) {
        this.name = trim(name);
    }
}
