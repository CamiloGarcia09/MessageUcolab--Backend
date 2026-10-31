package co.edu.uco.application.usecase.validator.application;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationIdDuplicatedRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationIdUuidRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationLanguageExistsRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationLanguageRequiredRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationNameDuplicatedRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationNameMaxLengthRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationNameRequiredRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationOrganizationExistsRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationOrganizationIdRequiredRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationOrganizationIdUuidRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationStateExistsRule;
import co.edu.uco.application.usecase.validator.application.rule.ApplicationStateRequiredRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class CreateApplicationCompositeValidator extends CompositeValidator<CreateApplicationDTO> {

    public CreateApplicationCompositeValidator(CatalogPort catalogPort,
                                               RecordExistsCatalogPort recordExistsCatalogPort,
                                               ApplicationRepository applicationRepository,
                                               OrganizationRepository organizationRepository) {
        super(List.of(
                new ApplicationIdUuidRule(catalogPort),
                new ApplicationNameRequiredRule(catalogPort),
                new ApplicationNameMaxLengthRule(catalogPort),
                new ApplicationOrganizationIdRequiredRule(catalogPort),
                new ApplicationOrganizationIdUuidRule(catalogPort),
                new ApplicationOrganizationExistsRule(catalogPort, organizationRepository),
                new ApplicationLanguageRequiredRule(catalogPort),
                new ApplicationLanguageExistsRule(catalogPort, recordExistsCatalogPort),
                new ApplicationStateRequiredRule(catalogPort),
                new ApplicationStateExistsRule(catalogPort, recordExistsCatalogPort),
                new ApplicationNameDuplicatedRule(catalogPort, applicationRepository),
                new ApplicationIdDuplicatedRule(catalogPort, applicationRepository)
        ), catalogPort);
    }
}
