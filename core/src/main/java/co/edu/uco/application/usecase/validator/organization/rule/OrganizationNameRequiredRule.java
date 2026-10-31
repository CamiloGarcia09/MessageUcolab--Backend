package co.edu.uco.application.usecase.validator.organization.rule;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class OrganizationNameRequiredRule extends RuleValidator<CreateOrganizationDTO> {

    public OrganizationNameRequiredRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateOrganizationDTO::getName, new TextRequiredSpecification()),
                MessageCatalogCodeEnum.FUN_147);
    }
}
