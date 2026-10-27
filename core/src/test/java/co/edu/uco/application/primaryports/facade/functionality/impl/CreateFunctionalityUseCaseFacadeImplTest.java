package co.edu.uco.application.primaryports.facade.functionality.impl;

import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.usecase.handling.HandlingCreateFunctionalityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateFunctionalityUseCaseFacadeImplTest {

    @Mock
    private HandlingCreateFunctionalityPort handlingCreateFunctionalityPort;

    private CreateFunctionalityUseCaseFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new CreateFunctionalityUseCaseFacadeImpl(handlingCreateFunctionalityPort);
    }

    @Test
    void execute_delegatesFunctionalityDto() {
        CreateFunctionalityDTO dto = new CreateFunctionalityDTO();

        facade.execute(dto);

        verify(handlingCreateFunctionalityPort).createFunctionality(dto);
    }
}
