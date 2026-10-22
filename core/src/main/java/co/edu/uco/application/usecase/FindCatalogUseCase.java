package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.usecase.handling.HandlingFindCatalogPort;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;

import java.util.List;
import java.util.stream.Collectors;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class FindCatalogUseCase implements HandlingFindCatalogPort {

    private final ApplicationCatalogRepository applicationCatalogRepository;
    private final EnvironmentCatalogRepository environmentCatalogRepository;
    private final FunctionalityCatalogRepository functionalityCatalogRepository;
    private final MessageTypeCatalogRepository messageTypeCatalogRepository;
    private final MessageCategoryCatalogRepository messageCategoryCatalogRepository;
    private final MessageStateCatalogRepository messageStateCatalogRepository;
    private final MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository;
    private final SessionApplicationAccessCompositeValidator accessValidator;

    public FindCatalogUseCase(
            ApplicationCatalogRepository applicationCatalogRepository,
            EnvironmentCatalogRepository environmentCatalogRepository,
            FunctionalityCatalogRepository functionalityCatalogRepository,
            MessageTypeCatalogRepository messageTypeCatalogRepository,
            MessageCategoryCatalogRepository messageCategoryCatalogRepository,
            MessageStateCatalogRepository messageStateCatalogRepository,
            MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository,
            SessionApplicationAccessCompositeValidator accessValidator) {
        this.applicationCatalogRepository = applicationCatalogRepository;
        this.environmentCatalogRepository = environmentCatalogRepository;
        this.functionalityCatalogRepository = functionalityCatalogRepository;
        this.messageTypeCatalogRepository = messageTypeCatalogRepository;
        this.messageCategoryCatalogRepository = messageCategoryCatalogRepository;
        this.messageStateCatalogRepository = messageStateCatalogRepository;
        this.messageEnvironmentStateCatalogRepository = messageEnvironmentStateCatalogRepository;
        this.accessValidator = accessValidator;
    }

    @Override
    public List<CatalogItemDTO> findApplications(final List<CatalogItemDTO> sessionApplications) {
        if (sessionApplications == null || sessionApplications.isEmpty()) {
            return List.of();
        }
        var authorizedIds = sessionApplications.stream()
                .map(CatalogItemDTO::getId)
                .filter(id -> new ValidUuidSpecification().isSatisfiedBy(id))
                .map(co.edu.uco.crosscutting.helpers.UtilUUID::getUUIDFromString)
                .collect(Collectors.toSet());
        return applicationCatalogRepository.findAll().stream()
                .filter(application -> authorizedIds.contains(application.getId()))
                .map(app -> CatalogItemDTO.create(app.getId().toString(), app.getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findEnvironmentsByApplication(final String applicationId,
                                                               final List<CatalogItemDTO> sessionApplications) {
        accessValidator.validate(new SessionApplicationValidationContext(sessionApplications, applicationId, null));
        return environmentCatalogRepository.findAllByApplicationId(applicationId).stream()
                .map(env -> CatalogItemDTO.create(env.getId().toString(), env.getType().getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findFunctionalitiesByApplication(final String applicationId,
                                                                  final List<CatalogItemDTO> sessionApplications) {
        accessValidator.validate(new SessionApplicationValidationContext(sessionApplications, applicationId, null));
        return functionalityCatalogRepository.findAllByApplicationId(applicationId).stream()
                .map(func -> CatalogItemDTO.create(func.getId().toString(), func.getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findMessageTypes() {
        return messageTypeCatalogRepository.findAll().stream()
                .map(type -> CatalogItemDTO.create(type.getId().toString(), type.getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findMessageCategories() {
        return messageCategoryCatalogRepository.findAll().stream()
                .map(cat -> CatalogItemDTO.create(cat.getId().toString(), cat.getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findMessageStates() {
        return messageStateCatalogRepository.findAll().stream()
                .map(st -> CatalogItemDTO.create(st.getId().toString(), st.getName()))
                .toList();
    }

    @Override
    public List<CatalogItemDTO> findMessageEnvironmentStates() {
        return messageEnvironmentStateCatalogRepository.findAll().stream()
                .map(st -> CatalogItemDTO.create(st.getId().toString(), st.getName()))
                .toList();
    }
}
