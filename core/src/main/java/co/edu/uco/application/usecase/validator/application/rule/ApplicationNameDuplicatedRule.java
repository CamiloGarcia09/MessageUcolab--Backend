package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class ApplicationNameDuplicatedRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationNameDuplicatedRule(CatalogPort catalogPort, ApplicationRepository applicationRepository) {
        super(catalogPort,
                dto -> !applicationRepository.existsByNameAndOrganization(dto.getName(), dto.getOrganizationId()),
                MessageCatalogCodeEnum.FUN_169);
    }
}
