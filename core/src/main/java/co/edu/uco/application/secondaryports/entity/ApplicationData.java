package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import lombok.Getter;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilObject.getDefaultIsNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

@Getter
public final class ApplicationData {
    private UUID id;
    private String name;
    private OrganizationEntity organization;

    public ApplicationData() {
        setId(getNewUUID());
        setName(EMPTY);
        setOrganization(null);
    }

    public ApplicationData(UUID id, String name) {
        this(id, name, null);
    }

    public ApplicationData(UUID id, String name, OrganizationEntity organization) {
        setId(id);
        setName(name);
        setOrganization(organization);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setName(String name) {
        this.name = trim(name);
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = getDefaultIsNullObject(organization, defaultOrganization());
    }

    public static ApplicationData build() {
        return new ApplicationData();
    }
    public static ApplicationData build(UUID id, String name) {
        return new ApplicationData(id, name);
    }

    public static ApplicationData build(UUID id, String name, OrganizationEntity organization) {
        return new ApplicationData(id, name, organization);
    }

    private static OrganizationEntity defaultOrganization() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(null);
        organization.setName(EMPTY);
        return organization;
    }
}
