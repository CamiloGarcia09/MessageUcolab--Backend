package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.message.rule.ConsumerLanguageCodeFormatRule;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.List;

public final class ApplicationBaseLanguageValidator extends CompositeValidator<String> {
    public ApplicationBaseLanguageValidator(CatalogPort catalog) {
        super(List.of(new ConsumerLanguageCodeFormatRule(catalog, MessageCatalogCodeEnum.FUN_219)), catalog);
    }
}
