package co.edu.uco.application.primaryports.dto.message;

import co.edu.uco.crosscutting.helpers.UtilObject;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

@Getter
public final class MessageEnvironmentDTO {
    private UUID id;
    private UUID messageId;
    private MessageDTO message;
    private UUID environmentTypeId;
    private UUID stateId;
    public MessageEnvironmentDTO(UUID id, UUID messageId, MessageDTO message, UUID environmentTypeId, UUID stateId) {
        setId(id);
        setMessageId(messageId);
        setMessage(message);
        setEnvironmentTypeId(environmentTypeId);
        setStateId(stateId);
    }
    public MessageEnvironmentDTO() {
        setId(getNewUUID());
        setMessageId(getNewUUID());
        setMessage(MessageDTO.create(EMPTY,EMPTY,EMPTY,EMPTY,EMPTY,EMPTY));
        setEnvironmentTypeId(getNewUUID());
        setStateId(getNewUUID());
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setMessageId(UUID messageId) {
        this.messageId = getDefaultUUID(messageId);
    }
    public void setEnvironmentTypeId(UUID environmentTypeId) {
        this.environmentTypeId = getDefaultUUID(environmentTypeId);
    }
    public void setStateId(UUID stateId) {
        this.stateId = getDefaultUUID(stateId);
    }
    public void setMessage(MessageDTO message) {
        this.message = UtilObject.getDefaultIsNullObject(message, MessageDTO.create(message.getCode(), message.getTitle(),
                message.getContent(), message.getType(), message.getCategory(),
                message.getFunctionality()));
    }
    public static MessageEnvironmentDTO create(UUID id, UUID messageId, MessageDTO message, UUID environmentTypeId, UUID stateId) {
        return new MessageEnvironmentDTO(id, messageId, message, environmentTypeId, stateId);
    }
}
