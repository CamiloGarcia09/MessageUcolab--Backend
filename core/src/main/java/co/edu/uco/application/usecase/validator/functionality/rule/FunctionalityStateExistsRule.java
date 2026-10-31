package co.edu.uco.application.usecase.validator.functionality.rule;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.CatalogReferenceExistsSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class FunctionalityStateExistsRule extends RuleValidator<CreateFunctionalityDTO> {

    public FunctionalityStateExistsRule(CatalogPort catalogPort, RecordExistsCatalogPort recordExistsCatalogPort) {
        super(catalogPort,
                Specifications.field(CreateFunctionalityDTO::getStateId,
                        new CatalogReferenceExistsSpecification(recordExistsCatalogPort, ReferenceCatalog.FUNCTIONALITY_STATE)),
                MessageCatalogCodeEnum.FUN_185);
    }
}
