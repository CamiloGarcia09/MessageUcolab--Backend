package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class ApplicationOrganizationExistsRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationOrganizationExistsRule(CatalogPort catalogPort, OrganizationRepository organizationRepository) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getOrganizationId,
                        organizationId -> new ValidUuidSpecification().isSatisfiedBy(organizationId)
                                && organizationRepository.findById(getUUIDFromString(organizationId)).isPresent()),
                MessageCatalogCodeEnum.FUN_151);
    }
}
