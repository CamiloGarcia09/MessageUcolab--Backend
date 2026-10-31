package co.edu.uco.application.usecase.handling;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;

public interface HandlingCreateApplicationPort {
    void createApplication(CreateApplicationDTO applicationDTO);
}
