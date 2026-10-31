package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import org.springframework.stereotype.Component;

@Component
public final class FindMessageCodeValidator extends RuleValidator<String> {

    public FindMessageCodeValidator(CatalogPort catalogPort) {
        super(catalogPort, new TextRequiredSpecification(), MessageCatalogCodeEnum.FUN_040);
    }
}
