package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import java.util.Map;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.IMP_006;

public final class ImportDuplicateCodeRule extends RuleValidator<String> {
    public ImportDuplicateCodeRule(CatalogPort catalog, Map<String, Long> counts) {
        super(catalog, normalized -> counts.getOrDefault(normalized, 0L) <= 1, IMP_006);
    }
}
