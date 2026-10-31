package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class FunctionalityApplicationExistsRule extends RuleValidator<CreateFunctionalityDTO> {

    public FunctionalityApplicationExistsRule(CatalogPort catalogPort, ApplicationRepository applicationRepository) {
        super(catalogPort,
                Specifications.field(CreateFunctionalityDTO::getApplicationId, applicationRepository::existsById),
                MessageCatalogCodeEnum.FUN_181);
    }
}
