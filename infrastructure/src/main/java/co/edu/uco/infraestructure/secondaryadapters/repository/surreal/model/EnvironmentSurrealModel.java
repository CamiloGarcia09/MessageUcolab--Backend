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
public final class EnvironmentSurrealModel {

    private UUID id;
    private String applicationId;
    private String typeId;
    private String stateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EnvironmentSurrealModel(UUID id, String applicationId, String typeId,
                                   String stateId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        setId(id);
        setApplicationId(applicationId);
        setTypeId(typeId);
        setStateId(stateId);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }

    public EnvironmentSurrealModel() {
        setId(getNewUUID());
        setApplicationId(EMPTY);
        setTypeId(EMPTY);
        setStateId(EMPTY);
        setCreatedAt(nowUtc());
        setUpdatedAt(nowUtc());
    }

    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = trim(applicationId);
    }

    public void setTypeId(String typeId) {
        this.typeId = trim(typeId);
    }

    public void setStateId(String stateId) {
        this.stateId = trim(stateId);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = getDefaultTimeIfNull(createdAt);
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = getDefaultTimeIfNull(updatedAt);
    }

    public static EnvironmentSurrealModel build() {
        return new EnvironmentSurrealModel();
    }
}
