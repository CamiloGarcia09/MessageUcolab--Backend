package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationNameRequiredRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationNameRequiredRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getName, new TextRequiredSpecification()),
                MessageCatalogCodeEnum.FUN_161);
    }
}
