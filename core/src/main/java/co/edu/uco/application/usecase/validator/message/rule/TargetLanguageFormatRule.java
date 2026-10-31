package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.RegexMatchSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class TargetLanguageFormatRule extends RuleValidator<String> {

    private static final RegexMatchSpecification FORMAT =
            new RegexMatchSpecification("^[a-zA-Z][a-zA-Z\\s_-]{1,49}$");

    public TargetLanguageFormatRule(CatalogPort catalogPort) {
        super(catalogPort, language -> FORMAT.isSatisfiedBy(trim(language)), MessageCatalogCodeEnum.FUN_045);
    }
}
