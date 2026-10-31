package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.CatalogReferenceExistsSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationLanguageExistsRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationLanguageExistsRule(CatalogPort catalogPort, RecordExistsCatalogPort recordExistsCatalogPort) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getLanguageId,
                        new CatalogReferenceExistsSpecification(recordExistsCatalogPort, ReferenceCatalog.LANGUAGE_BASE)),
                MessageCatalogCodeEnum.FUN_164);
    }
}
