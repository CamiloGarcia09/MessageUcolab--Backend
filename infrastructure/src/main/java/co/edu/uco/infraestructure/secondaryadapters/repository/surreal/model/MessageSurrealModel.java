package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilDate.getDefaultTimeIfNull;
import static co.edu.uco.crosscutting.helpers.UtilDate.nowUtc;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

@Getter
public final class MessageSurrealModel {

    private UUID id;
    private String code;
    private String title;
    private String content;
    private String typeId;
    private String categoryId;
    private String statusId;
    private String applicationId;
    private String functionalityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MessageSurrealModel(UUID id, String code, String title, String content, String typeId,
                               String categoryId, String statusId, String applicationId, String functionalityId,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        setId(id);
        setCode(code);
        setTitle(title);
        setContent(content);
        setTypeId(typeId);
        setCategoryId(categoryId);
        setStatusId(statusId);
        setApplicationId(applicationId);
        setFunctionalityId(functionalityId);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }

    public MessageSurrealModel() {
        setId(getNewUUID());
        setCode(EMPTY);
        setTitle(EMPTY);
        setContent(EMPTY);
        setTypeId(EMPTY);
        setCategoryId(EMPTY);
        setStatusId(EMPTY);
        setApplicationId(EMPTY);
        setFunctionalityId(EMPTY);
        setCreatedAt(nowUtc());
        setUpdatedAt(nowUtc());
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

    public void setTypeId(String typeId) {
        this.typeId = trim(typeId);
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = trim(categoryId);
    }

    public void setStatusId(String statusId) {
        this.statusId = trim(statusId);
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = trim(applicationId);
    }

    public void setFunctionalityId(String functionalityId) {
        this.functionalityId = trim(functionalityId);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = getDefaultTimeIfNull(createdAt);
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = getDefaultTimeIfNull(updatedAt);
    }

    public static MessageSurrealModel build() {
        return new MessageSurrealModel();
    }
}
