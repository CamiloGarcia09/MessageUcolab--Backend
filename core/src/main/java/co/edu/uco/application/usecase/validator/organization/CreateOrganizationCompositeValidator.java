package co.edu.uco.application.usecase.validator.organization;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.organization.rule.OrganizationNameDuplicatedRule;
import co.edu.uco.application.usecase.validator.organization.rule.OrganizationNameMaxLengthRule;
import co.edu.uco.application.usecase.validator.organization.rule.OrganizationNameRequiredRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class CreateOrganizationCompositeValidator extends CompositeValidator<CreateOrganizationDTO> {

    public CreateOrganizationCompositeValidator(CatalogPort catalogPort,
                                                OrganizationRepository organizationRepository) {
        super(List.of(
                new OrganizationNameRequiredRule(catalogPort),
                new OrganizationNameMaxLengthRule(catalogPort),
                new OrganizationNameDuplicatedRule(catalogPort, organizationRepository)
        ), catalogPort);
    }
}
