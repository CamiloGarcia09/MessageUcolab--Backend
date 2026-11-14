package co.edu.uco.application.primaryports.facade.serviceidentity.impl;

import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.ServiceIdentityDTO;
import co.edu.uco.application.primaryports.facade.serviceidentity.ServiceIdentityUseCaseFacade;
import co.edu.uco.application.usecase.ManageServiceIdentitiesUseCase;

import java.util.List;

public final class ServiceIdentityUseCaseFacadeImpl implements ServiceIdentityUseCaseFacade {

    private final ManageServiceIdentitiesUseCase useCase;

    public ServiceIdentityUseCaseFacadeImpl(final ManageServiceIdentitiesUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public List<ServiceIdentityDTO> list(final String applicationId) {
        return useCase.list(applicationId);
    }

    @Override
    public void register(final String applicationId, final RegisterServiceIdentityDTO request) {
        useCase.register(applicationId, request);
    }

    @Override
    public void remove(final String applicationId, final String clientId) {
        useCase.remove(applicationId, clientId);
    }
}
