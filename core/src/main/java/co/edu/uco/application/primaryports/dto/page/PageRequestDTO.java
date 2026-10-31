package co.edu.uco.application.primaryports.dto.page;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class PageRequestDTO {
    private String page;
    private String sort;
    private String columnSort;
    private String size;
    private String filterCode;
    private String filterTitle;
    private String filterCategory;
    private String filterFunctionality;
    private String filterType;
    private String filterStatus;
    // Ids de ambiente separados por coma; vacio muestra todos los de la aplicacion.
    private String filterEnvironments;
    private String filterStatusId;
    private String filterFunctionalityId;

    public PageRequestDTO(String page, String sort, String columnSort, String size,
                          String filterCode, String filterTitle, String filterCategory,
                          String filterFunctionality, String filterType, String filterStatus,
                          String filterEnvironments) {
        this(page, sort, columnSort, size, filterCode, filterTitle, filterCategory,
                filterFunctionality, filterType, filterStatus, filterEnvironments, null, null);
    }
}
