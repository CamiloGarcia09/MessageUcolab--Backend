package co.edu.uco.application.primaryports.facade.serviceidentity;

import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.ServiceIdentityDTO;

import java.util.List;

public interface ServiceIdentityUseCaseFacade {

    List<ServiceIdentityDTO> list(String applicationId);

    void register(String applicationId, RegisterServiceIdentityDTO request);

    void remove(String applicationId, String clientId);
}
