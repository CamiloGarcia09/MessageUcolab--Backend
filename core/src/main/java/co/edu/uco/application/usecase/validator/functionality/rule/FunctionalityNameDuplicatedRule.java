package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class FunctionalityNameDuplicatedRule extends RuleValidator<CreateFunctionalityDTO> {

    public FunctionalityNameDuplicatedRule(CatalogPort catalogPort, FunctionalityRepository functionalityRepository) {
        super(catalogPort,
                functionality -> !functionalityRepository.existsByNameAndApplicationId(functionality.getName(),
                        functionality.getApplicationId()),
                MessageCatalogCodeEnum.FUN_186);
    }
}
