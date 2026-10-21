package co.edu.uco.application.primaryports.facade.session.impl;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.usecase.handling.HandlingSessionApplicationPort;

import java.util.List;

public final class SessionApplicationUseCaseFacadeImpl implements SessionApplicationUseCaseFacade {

    private final HandlingSessionApplicationPort handlingSessionApplicationPort;

    public SessionApplicationUseCaseFacadeImpl(final HandlingSessionApplicationPort handlingSessionApplicationPort) {
        this.handlingSessionApplicationPort = handlingSessionApplicationPort;
    }

    @Override
    public List<CatalogItemDTO> findCurrentUserApplications(final String session) {
        return handlingSessionApplicationPort.findCurrentUserApplications(session);
    }

    @Override
    public List<SessionApplicationDTO> findSessionApplications(final List<CatalogItemDTO> sessionApplications) {
        return handlingSessionApplicationPort.findSessionApplications(sessionApplications);
    }

    @Override
    public String resolveApplication(final List<CatalogItemDTO> sessionApplications, final String applicationId) {
        return handlingSessionApplicationPort.resolveApplication(sessionApplications, applicationId);
    }

    @Override
    public String resolveEnvironment(final List<CatalogItemDTO> sessionApplications,
                                     final String applicationId,
                                     final String environmentId) {
        return handlingSessionApplicationPort.resolveEnvironment(sessionApplications, applicationId, environmentId);
    }

    @Override
    public void authorizeApplication(final List<CatalogItemDTO> sessionApplications, final String applicationId) {
        handlingSessionApplicationPort.authorizeApplication(sessionApplications, applicationId);
    }
}
