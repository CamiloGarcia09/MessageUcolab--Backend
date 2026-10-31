package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class FunctionalityNameMaxLengthRule extends RuleValidator<CreateFunctionalityDTO> {

    private static final int NAME_MAX_LENGTH = 50;

    public FunctionalityNameMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateFunctionalityDTO::getName, new TextMaxLengthSpecification(NAME_MAX_LENGTH)),
                MessageCatalogCodeEnum.FUN_180);
    }
}
