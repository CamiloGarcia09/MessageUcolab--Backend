package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.ImportReference;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import java.util.List;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.IMP_005;

public final class ImportReferenceRule extends RuleValidator<String> {
    public ImportReferenceRule(CatalogPort catalog, List<ImportReference> references) {
        super(catalog, value -> value.isBlank() || references.stream().filter(ref -> ref.matches(value))
                .map(ImportReference::id).distinct().count() == 1, IMP_005);
    }
}
