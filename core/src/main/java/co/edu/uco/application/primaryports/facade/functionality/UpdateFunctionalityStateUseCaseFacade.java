package co.edu.uco.application.primaryports.facade.functionality;

import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;

public interface UpdateFunctionalityStateUseCaseFacade {
    void execute(String functionalityId, UpdateFunctionalityStateDTO dto);
}
