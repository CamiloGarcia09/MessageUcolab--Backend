package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.crosscutting.helpers.UtilObject;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class MessageEnvironmentData {
    private UUID id;
    private MessageData message;
    private EnvironmentTypeData environmentTypeData;
    private MessageEnvironmentStateData stateData;

    public MessageEnvironmentData(UUID id, MessageData message, EnvironmentTypeData environmentTypeData) {
        setId(id);
        setMessage(message);
        setEnvironmentTypeData(environmentTypeData);
    }
    public MessageEnvironmentData() {
        setId(UtilUUID.getNewUUID());
        setMessage(MessageData.build());
        setEnvironmentTypeData(EnvironmentTypeData.build());
    }
    public void setId(UUID id) {
        this.id = UtilUUID.getDefaultUUID(id);
    }
    public void setMessage(MessageData message) {
        this.message = UtilObject.getDefaultIsNullObject(message, MessageData.build());
    }
    public void setEnvironmentTypeData(EnvironmentTypeData environmentTypeData) {
        this.environmentTypeData = UtilObject.getDefaultIsNullObject(environmentTypeData, EnvironmentTypeData.build());
    }
    public void setStateData(MessageEnvironmentStateData stateData) {
        this.stateData = UtilObject.getDefaultIsNullObject(stateData, MessageEnvironmentStateData.build());
    }
}