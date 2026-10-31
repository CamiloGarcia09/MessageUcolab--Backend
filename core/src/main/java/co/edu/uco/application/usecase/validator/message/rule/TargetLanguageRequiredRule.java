package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class TargetLanguageRequiredRule extends RuleValidator<String> {

    public TargetLanguageRequiredRule(CatalogPort catalogPort) {
        super(catalogPort, new TextRequiredSpecification(), MessageCatalogCodeEnum.FUN_044);
    }
}
