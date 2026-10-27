package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.message.rule.ConsumerLanguageCodeFormatRule;
import co.edu.uco.application.usecase.validator.message.rule.TargetLanguageRequiredRule;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.List;

public final class ConsumerTranslationLanguageValidator extends CompositeValidator<String> {
    public ConsumerTranslationLanguageValidator(CatalogPort catalog) {
        super(List.of(new TargetLanguageRequiredRule(catalog),
                new ConsumerLanguageCodeFormatRule(catalog, MessageCatalogCodeEnum.FUN_045)), catalog);
    }
}
