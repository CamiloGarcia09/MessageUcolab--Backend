package co.edu.uco.application.secondaryports.entity;


import co.edu.uco.crosscutting.helpers.UtilObject;
import co.edu.uco.crosscutting.helpers.UtilUUID;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;

@Getter
public final class FunctionalityData {
    private UUID id;
    private String name;
    private ApplicationData application;
    private UUID stateId = DEFAULT_UUID;
    private String state = EMPTY;
    public FunctionalityData() {
        setId(UUID.randomUUID());
        setName(EMPTY);
    }
    public FunctionalityData(UUID id, String name, ApplicationData application) {
        setId(id);
        setName(name);
        setApplication(application);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setName(String name) {
        this.name = trim(name);
    }
    public void setApplication(ApplicationData application) {this.application = UtilObject.getDefaultIsNullObject(application, ApplicationData.build());}
    public void setStateId(UUID stateId) {
        this.stateId = getDefaultUUID(stateId);
    }
    public void setState(String state) {
        this.state = trim(state);
    }
    public static FunctionalityData build() {
        return new FunctionalityData();
    }
    public static FunctionalityData build(String name) {
        return new FunctionalityData(UtilUUID.getNewUUID(), name, ApplicationData.build());
    }
}
