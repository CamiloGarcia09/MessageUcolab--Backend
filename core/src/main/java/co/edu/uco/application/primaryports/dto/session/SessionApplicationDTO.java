package co.edu.uco.application.primaryports.dto.session;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class SessionApplicationDTO {

    private String id;
    private String name;
    private boolean registered;
    private CatalogItemDTO organization;
}
