package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;

public interface HandlingCreateFunctionalityPort {
    void createFunctionality(CreateFunctionalityDTO functionalityDTO);
}
