package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationComparison;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;

public final class ConsumerTranslationUnchangedRule extends RuleValidator<ConsumerTranslationComparison> {
    public ConsumerTranslationUnchangedRule(CatalogPort catalog) {
        super(catalog, comparison -> comparison.before().values().equals(comparison.after().values()),
                MessageCatalogCodeEnum.FUN_220, ConflictException::buildUserException);
    }
}
