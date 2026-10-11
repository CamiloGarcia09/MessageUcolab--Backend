package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.crosscutting.helpers.UtilObject;
import co.edu.uco.crosscutting.helpers.UtilText;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;
import java.time.Instant;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilObject.getDefaultIsNullObject;

@Getter
public final class MessageData {
    private UUID id;
    private String code;
    private String title;
    private String content;
    private MessageTypeData type;
    private MessageCategoryData category;
    private StatusMessageData status;
    private String application;
    private FunctionalityData functionality;
    private Instant createdAt;
    public MessageData() {
        setId(UtilUUID.getNewUUID());
        setCode(UtilText.EMPTY);
        setTitle(UtilText.EMPTY);
        setContent(UtilText.EMPTY);
        setApplication(UtilText.EMPTY);
        setType(MessageTypeData.build());
        setCategory(MessageCategoryData.build());
        setStatus(StatusMessageData.build());
        setFunctionality(FunctionalityData.build());
    }
    public MessageData(UUID id, String code, String title, String content, MessageTypeData type,
                       MessageCategoryData category,  String application, FunctionalityData functionality) {
        setId(id);
        setCode(code);
        setTitle(title);
        setContent(content);
        setType(type);
        setStatus(StatusMessageData.build());
        setApplication(application);
        setCategory(category);
        setFunctionality(functionality);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setCode(String code) {
        this.code = trim(code);
    }
    public void setTitle(String title) {
        this.title = trim(title);
    }
    public void setContent(String content) {
        this.content = trim(content);
    }
    public void setType(MessageTypeData type) {this.type = UtilObject.getDefaultIsNullObject(type,MessageTypeData.build());}
    public void setCategory(MessageCategoryData category) {this.category = getDefaultIsNullObject(category, MessageCategoryData.build());}
    public void setStatus(StatusMessageData status) {this.status = getDefaultIsNullObject(status, StatusMessageData.build());}
    public void setApplication(String application) {
        this.application = trim(application);
    }
    public void setFunctionality(FunctionalityData functionality) {this.functionality = getDefaultIsNullObject(functionality, FunctionalityData.build());}
    public static MessageData build() {
        return new MessageData();
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
