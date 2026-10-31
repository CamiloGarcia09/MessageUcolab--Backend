package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.OrganizationRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.handling.HandlingCreateOrganizationPort;
import co.edu.uco.application.usecase.validator.organization.CreateOrganizationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getNewUUID;

public final class CreateOrganizationUseCase implements HandlingCreateOrganizationPort {

    private final OrganizationRepository organizationRepository;
    private final CreateOrganizationCompositeValidator validator;
    private final LoggingPort log;

    public CreateOrganizationUseCase(final OrganizationRepository organizationRepository,
                                     final CreateOrganizationCompositeValidator validator,
                                     final LoggingPortFactory loggerFactory) {
        this.organizationRepository = organizationRepository;
        this.validator = validator;
        this.log = loggerFactory.getLogger(CreateOrganizationUseCase.class);
    }

    @Override
    public void createOrganization(final CreateOrganizationDTO organizationDTO) {
        validator.validate(organizationDTO);

        try {
            final OrganizationEntity organization = new OrganizationEntity();
            organization.setId(getNewUUID());
            organization.setName(organizationDTO.getName());
            organizationRepository.create(organization);
            log.info(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_091.getCode()));
        } catch (final CrossWordsException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_092.getCode()), exception);
            throw BusinessException.buildTechnicalException(
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_092.getCode()), exception, ExceptionLocation.APPLICATION);
        }
    }
}
