package co.edu.uco.application.primaryports.dto.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class ApplicationCatalogItemDTO extends CatalogItemDTO {

    private final CatalogItemDTO language;

    public ApplicationCatalogItemDTO(String id, String name, CatalogItemDTO language) {
        super(id, name);
        this.language = language;
    }
}
