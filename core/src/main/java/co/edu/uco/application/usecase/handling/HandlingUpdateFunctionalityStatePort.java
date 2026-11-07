package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;

public interface HandlingUpdateFunctionalityStatePort {
    void updateState(String functionalityId, UpdateFunctionalityStateDTO dto);
}
