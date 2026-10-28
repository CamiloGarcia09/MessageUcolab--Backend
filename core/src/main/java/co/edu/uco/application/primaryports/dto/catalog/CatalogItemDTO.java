package co.edu.uco.application.primaryports.dto.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Getter
@EqualsAndHashCode
@ToString
public sealed class CatalogItemDTO permits FunctionalityCatalogItemDTO, ApplicationCatalogItemDTO {

    private final String id;
    private final String name;

    public CatalogItemDTO(String id, String name) {
        this.id = trim(id);
        this.name = trim(name);
    }

    public static CatalogItemDTO create(String id, String name) {
        return new CatalogItemDTO(id, name);
    }
}
