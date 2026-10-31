package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationLanguageRequiredRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationLanguageRequiredRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getLanguageId, new TextRequiredSpecification()),
                MessageCatalogCodeEnum.FUN_163);
    }
}
