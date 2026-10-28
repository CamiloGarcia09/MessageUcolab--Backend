package co.edu.uco.application.usecase.validator.message.importing;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import java.time.Instant;
import java.util.List;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.*;
import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isProduction;

public final class ImportConfirmationCompositeValidator
        extends CompositeValidator<ImportConfirmationCompositeValidator.Context> {
    public record Context(MessageImportPreview preview, boolean productionConfirmed) { }

    public ImportConfirmationCompositeValidator(CatalogPort catalog, EnvironmentRepository environments) {
        super(List.of(new UnexpiredRule(catalog), new ValidRowsRule(catalog),
                new ProductionRule(catalog, environments)), catalog);
    }

    private static final class UnexpiredRule extends RuleValidator<Context> {
        private UnexpiredRule(CatalogPort catalog) {
            super(catalog, value -> value.preview().expiresAt().isAfter(Instant.now()), IMP_007);
        }
    }

    private static final class ValidRowsRule extends RuleValidator<Context> {
        private ValidRowsRule(CatalogPort catalog) {
            super(catalog, value -> !value.preview().rows().isEmpty()
                    && value.preview().rows().stream().allMatch(row -> row.issues().isEmpty()), IMP_008);
        }
    }

    private static final class ProductionRule extends RuleValidator<Context> {
        private ProductionRule(CatalogPort catalog, EnvironmentRepository environments) {
            super(catalog, value -> value.productionConfirmed() || value.preview().environmentIds().stream()
                    .noneMatch(id -> environments.findById(id)
                            .map(env -> isProduction(env.getType().getName())).orElse(false)), IMP_009);
        }
    }
}
