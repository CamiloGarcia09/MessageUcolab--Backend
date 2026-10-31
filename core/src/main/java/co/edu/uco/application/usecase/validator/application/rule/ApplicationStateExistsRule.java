package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.CatalogReferenceExistsSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationStateExistsRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationStateExistsRule(CatalogPort catalogPort, RecordExistsCatalogPort recordExistsCatalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getStateId,
                        new CatalogReferenceExistsSpecification(recordExistsCatalogPort, ReferenceCatalog.APPLICATION_STATE)),
                MessageCatalogCodeEnum.FUN_168);
    }
}
