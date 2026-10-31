package co.edu.uco.application.usecase.validator.organization.rule;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class OrganizationNameDuplicatedRule extends RuleValidator<CreateOrganizationDTO> {

    public OrganizationNameDuplicatedRule(CatalogPort catalogPort, OrganizationRepository organizationRepository) {
        super(catalogPort,
                Specifications.field(CreateOrganizationDTO::getName,
                        name -> organizationRepository.findByName(name).isEmpty()),
                MessageCatalogCodeEnum.FUN_149);
    }
}
