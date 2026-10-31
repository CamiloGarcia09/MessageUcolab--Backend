package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.ImportRow;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import java.util.List;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.IMP_002;

public final class ImportRowsRule extends RuleValidator<List<ImportRow>> {
    public static final int MAX_ROWS = 500;

    public ImportRowsRule(CatalogPort catalog) {
        super(catalog, rows -> rows != null && !rows.isEmpty() && rows.size() <= MAX_ROWS
                && rows.stream().allMatch(row -> row != null && row.rowNumber() >= 2
                && row.rowNumber() <= MAX_ROWS + 1)
                && rows.stream().map(ImportRow::rowNumber).distinct().count() == rows.size(), IMP_002);
    }
}
