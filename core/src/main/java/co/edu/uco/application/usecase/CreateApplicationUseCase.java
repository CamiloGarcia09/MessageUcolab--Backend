package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentReferenceCatalogRepository;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import co.edu.uco.application.usecase.validator.application.CreateApplicationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.helpers.UtilUUID;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_175;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_177;
import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class CreateApplicationUseCase implements HandlingCreateApplicationPort {

    private static final List<String> DEFAULT_ENVIRONMENT_TYPES = List.of("Develop", "Testing", "Production");

    private final ApplicationRepository applicationRepository;
    private final EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository;
    private final CreateApplicationCompositeValidator validator;
    private final CatalogPort catalogPort;
    private final LoggingPort log;

    public CreateApplicationUseCase(ApplicationRepository applicationRepository,
                                    EnvironmentReferenceCatalogRepository environmentReferenceCatalogRepository,
                                    CreateApplicationCompositeValidator validator,
                                    CatalogPort catalogPort,
                                    LoggingPortFactory loggerFactory) {
        this.applicationRepository = applicationRepository;
        this.environmentReferenceCatalogRepository = environmentReferenceCatalogRepository;
        this.validator = validator;
        this.catalogPort = catalogPort;
        this.log = loggerFactory.getLogger(CreateApplicationUseCase.class);
    }

    @Override
    public void createApplication(CreateApplicationDTO dto) {
        validator.validate(dto);

        try {
            OrganizationEntity organization = new OrganizationEntity();
            organization.setId(getUUIDFromString(dto.getOrganizationId()));
            organization.setName("");
            var applicationId = UtilUUID.getNewUUID();
            var application = new ApplicationData(applicationId, dto.getName(), organization);
            var environments = defaultEnvironments(application);
            var environmentStateId = activeEnvironmentStateId();
            applicationRepository.createWithEnvironments(
                    application,
                    dto.getLanguageId(),
                    dto.getStateId(),
                    environments,
                    environmentStateId.toString()
            );
            log.info("Application created successfully with name: {}", dto.getName());
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Error creating application in repository", ex);
            throw BusinessException.buildTechnicalException(
                    "Error al crear la aplicación", ex, ExceptionLocation.APPLICATION);
        }
    }

    private List<EnvironmentData> defaultEnvironments(final ApplicationData application) {
        var availableTypes = environmentReferenceCatalogRepository.findAllTypes();
        return DEFAULT_ENVIRONMENT_TYPES.stream()
                .map(typeName -> findType(availableTypes, typeName))
                .map(type -> new EnvironmentData(UtilUUID.getNewUUID(), application, type))
                .toList();
    }

    private EnvironmentTypeData findType(final List<EnvironmentTypeData> availableTypes, final String typeName) {
        if (isNullObject(availableTypes)) {
            throw missingEnvironmentType();
        }
        return availableTypes.stream()
                .filter(type -> !isNullObject(type) && typeName.equalsIgnoreCase(type.getName()))
                .findFirst()
                .orElseThrow(this::missingEnvironmentType);
    }

    private UUID activeEnvironmentStateId() {
        return environmentReferenceCatalogRepository.findStateIdByName(STATE_ACTIVE)
                .orElseThrow(() -> BusinessRuleException.buildUserException(
                        catalogPort.getMessage(FUN_177.getCode())));
    }

    private BusinessRuleException missingEnvironmentType() {
        return BusinessRuleException.buildUserException(catalogPort.getMessage(FUN_175.getCode()));
    }

}
