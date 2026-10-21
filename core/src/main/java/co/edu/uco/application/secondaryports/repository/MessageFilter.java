package co.edu.uco.application.secondaryports.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class MessageFilter {
    private String code;
    private String title;
    private String category;
    private String functionality;
    private String type;
    private String status;
    // Ambientes a mostrar; vacio significa todos los de la aplicacion.
    private List<String> environmentIds;

    public boolean hasFilters() {
        return notBlank(code) || notBlank(title) || notBlank(category)
                || notBlank(functionality) || notBlank(type) || notBlank(status);
    }

    private static boolean notBlank(final String value) {
        return value != null && !value.isBlank();
    }
}
