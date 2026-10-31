package co.edu.uco.application.usecase.domain;

import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

@Getter
public final class MessageStatusDomain {
    private UUID id;
    private String name;

    public MessageStatusDomain(UUID id, String name) {
        setId(id);
        setName(name);
    }

    public static MessageStatusDomain create(UUID id, String name) {
        return new MessageStatusDomain(id, name);
    }

    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }

    public void setName(String name) {
        this.name = trim(name);
    }
}