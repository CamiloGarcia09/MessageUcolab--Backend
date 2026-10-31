package co.edu.uco.application.primaryports.facade.application;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;

public interface CreateApplicationUseCaseFacade {
    void execute(CreateApplicationDTO createApplicationDTO);
}
