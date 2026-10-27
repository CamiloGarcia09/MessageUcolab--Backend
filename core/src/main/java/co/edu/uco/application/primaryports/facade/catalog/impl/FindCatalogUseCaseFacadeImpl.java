package co.edu.uco.application.primaryports.facade.catalog.impl;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingFindCatalogPort;

import java.util.List;

public final class FindCatalogUseCaseFacadeImpl implements FindCatalogUseCaseFacade {

    private final HandlingFindCatalogPort handlingFindCatalogPort;

    public FindCatalogUseCaseFacadeImpl(HandlingFindCatalogPort handlingFindCatalogPort) {
        this.handlingFindCatalogPort = handlingFindCatalogPort;
    }

    @Override
    public List<CatalogItemDTO> findApplications(final List<CatalogItemDTO> sessionApplications) {
        return handlingFindCatalogPort.findApplications(sessionApplications);
    }

    @Override
    public List<CatalogItemDTO> findEnvironmentsByApplication(final String applicationId,
                                                               final List<CatalogItemDTO> sessionApplications) {
        return handlingFindCatalogPort.findEnvironmentsByApplication(applicationId, sessionApplications);
    }

    @Override
    public List<CatalogItemDTO> findFunctionalitiesByApplication(final String applicationId,
                                                                  final List<CatalogItemDTO> sessionApplications) {
        return handlingFindCatalogPort.findFunctionalitiesByApplication(applicationId, sessionApplications);
    }

    @Override
    public List<CatalogItemDTO> findMessageTypes() {
        return handlingFindCatalogPort.findMessageTypes();
    }

    @Override
    public List<CatalogItemDTO> findMessageCategories() {
        return handlingFindCatalogPort.findMessageCategories();
    }

    @Override
    public List<CatalogItemDTO> findMessageStates() {
        return handlingFindCatalogPort.findMessageStates();
    }

    @Override
    public List<CatalogItemDTO> findMessageEnvironmentStates() {
        return handlingFindCatalogPort.findMessageEnvironmentStates();
    }
}
