package co.edu.uco.application.secondaryports.entity;

import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.getDefaultIsNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

@Getter
public final class EnvironmentData {
    private UUID id;
    private ApplicationData application;
    private EnvironmentTypeData type;
    public EnvironmentData(UUID id, ApplicationData application, EnvironmentTypeData type) {
        setId(id);
        setApplication(application);
        setType(type);
    }
    public EnvironmentData() {
        setId(getNewUUID());
        setApplication(ApplicationData.build());
        setType(EnvironmentTypeData.build());
    }
    public void setId(UUID id) { this.id = getDefaultUUID(id);}
    public void setApplication(ApplicationData application) { this.application = getDefaultIsNullObject(application, ApplicationData.build());}
    public void setType(EnvironmentTypeData type) { this.type = getDefaultIsNullObject(type, EnvironmentTypeData.build()); }
    public static EnvironmentData build() {
        return new EnvironmentData();
    }
}
