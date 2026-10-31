package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.RegexMatchSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ConsumerLanguageCodeFormatRule extends RuleValidator<String> {
    private static final RegexMatchSpecification FORMAT =
            new RegexMatchSpecification("^(?=.{2,50}$)[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$");

    public ConsumerLanguageCodeFormatRule(CatalogPort catalog, MessageCatalogCodeEnum code) {
        super(catalog, FORMAT, code);
    }
}
