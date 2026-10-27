package co.edu.uco.application.primaryports.facade.catalog;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;

import java.util.List;

public interface FindCatalogUseCaseFacade {
    List<CatalogItemDTO> findApplications(List<CatalogItemDTO> sessionApplications);
    List<CatalogItemDTO> findEnvironmentsByApplication(String applicationId, List<CatalogItemDTO> sessionApplications);
    List<CatalogItemDTO> findFunctionalitiesByApplication(String applicationId, List<CatalogItemDTO> sessionApplications);
    List<CatalogItemDTO> findMessageTypes();
    List<CatalogItemDTO> findMessageCategories();
    List<CatalogItemDTO> findMessageStates();
    List<CatalogItemDTO> findMessageEnvironmentStates();
}
