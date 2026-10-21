package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;

import java.util.List;

public interface HandlingSessionApplicationPort {

    List<CatalogItemDTO> findCurrentUserApplications(String session);

    List<SessionApplicationDTO> findSessionApplications(List<CatalogItemDTO> sessionApplications);

    String resolveApplication(List<CatalogItemDTO> sessionApplications, String applicationId);

    String resolveEnvironment(List<CatalogItemDTO> sessionApplications, String applicationId, String environmentId);

    void authorizeApplication(List<CatalogItemDTO> sessionApplications, String applicationId);
}
