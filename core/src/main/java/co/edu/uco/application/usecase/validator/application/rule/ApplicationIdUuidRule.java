package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

/**
 * El id es opcional; cuando se envia debe ser un UUID valido.
 */
public final class ApplicationIdUuidRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationIdUuidRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getId,
                        id -> !new TextRequiredSpecification().isSatisfiedBy(id)
                                || new ValidUuidSpecification().isSatisfiedBy(id)),
                MessageCatalogCodeEnum.FUN_038);
    }
}
