package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationNameMaxLengthRule extends RuleValidator<CreateApplicationDTO> {

    private static final int NAME_MAX_LENGTH = 50;

    public ApplicationNameMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getName, new TextMaxLengthSpecification(NAME_MAX_LENGTH)),
                MessageCatalogCodeEnum.FUN_162);
    }
}
