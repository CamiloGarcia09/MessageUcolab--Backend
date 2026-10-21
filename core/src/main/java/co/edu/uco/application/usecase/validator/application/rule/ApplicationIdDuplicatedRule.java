package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;

/**
 * La creacion persiste con UPSERT: un id explicito ya registrado sobrescribiria la aplicacion.
 */
public final class ApplicationIdDuplicatedRule extends RuleValidator<CreateApplicationDTO> {

    public ApplicationIdDuplicatedRule(CatalogPort catalogPort, ApplicationRepository applicationRepository) {
        super(catalogPort,
                Specifications.field(CreateApplicationDTO::getId,
                        id -> !new TextRequiredSpecification().isSatisfiedBy(id)
                                || !applicationRepository.existsById(id)),
                MessageCatalogCodeEnum.FUN_211,
                ConflictException::buildUserException);
    }
}
