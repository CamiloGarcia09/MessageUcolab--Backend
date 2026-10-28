package co.edu.uco.application.usecase.domain.message.importing;

import java.util.List;

public record ImportReferences(List<ImportReference> functionalities, List<ImportReference> types,
                               List<ImportReference> categories, List<ImportReference> states) {
}
