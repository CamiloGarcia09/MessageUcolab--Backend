package co.edu.uco.application.usecase.validator.organization.rule;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class OrganizationNameMaxLengthRule extends RuleValidator<CreateOrganizationDTO> {

    private static final int NAME_MAX_LENGTH = 50;

    public OrganizationNameMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateOrganizationDTO::getName, new TextMaxLengthSpecification(NAME_MAX_LENGTH)),
                MessageCatalogCodeEnum.FUN_148);
    }
}
