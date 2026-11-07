package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.application.usecase.handling.HandlingUpdateFunctionalityStatePort;
import co.edu.uco.application.usecase.validator.functionality.FunctionalityStateChange;
import co.edu.uco.application.usecase.validator.functionality.UpdateFunctionalityStateCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;

import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isInactive;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_185;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_228;

public final class UpdateFunctionalityStateUseCase implements HandlingUpdateFunctionalityStatePort {
    private final UpdateFunctionalityStateRepository repository;
    private final MessageStateCatalogRepository messageStates;
    private final UpdateFunctionalityStateCompositeValidator validator;
    private final MessageCatalogStrategy messageCatalog;
    private final CatalogPort catalog;

    public UpdateFunctionalityStateUseCase(UpdateFunctionalityStateRepository repository,
            MessageStateCatalogRepository messageStates, UpdateFunctionalityStateCompositeValidator validator,
            MessageCatalogStrategy messageCatalog, CatalogPort catalog) {
        this.repository = repository;
        this.messageStates = messageStates;
        this.validator = validator;
        this.messageCatalog = messageCatalog;
        this.catalog = catalog;
    }

    @Override
    public void updateState(String functionalityId, UpdateFunctionalityStateDTO dto) {
        var change = dto == null ? null : new FunctionalityStateChange(functionalityId, dto.applicationId(), dto.stateId());
        validator.validate(change);
        var state = repository.findStateNameById(change.stateId())
                .orElseThrow(() -> BusinessRuleException.buildUserException(catalog.getMessage(FUN_185.getCode())));
        String inactiveMessageStateId = isInactive(state) ? inactiveMessageStateId() : null;
        repository.updateState(change.functionalityId(), change.applicationId(), change.stateId(), inactiveMessageStateId)
                .forEach(messageCatalog::evictMessage);
    }

    private String inactiveMessageStateId() {
        return messageStates.findAll().stream()
                .filter(state -> state != null && isInactive(state.getName()))
                .map(state -> state.getId().toString())
                .findFirst()
                .orElseThrow(() -> BusinessException.buildTechnicalException(catalog.getMessage(FUN_228.getCode()),
                        null, ExceptionLocation.APPLICATION));
    }
}
