package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.message.rule.ConsumerTranslationUnchangedRule;

import java.util.List;

public final class ConsumerTranslationConsistencyValidator extends CompositeValidator<ConsumerTranslationComparison> {
    public ConsumerTranslationConsistencyValidator(CatalogPort catalog) {
        super(List.of(new ConsumerTranslationUnchangedRule(catalog)), catalog);
    }
}
