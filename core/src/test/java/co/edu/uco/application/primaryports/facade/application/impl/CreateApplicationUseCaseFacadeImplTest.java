package co.edu.uco.application.primaryports.facade.application.impl;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.usecase.handling.HandlingCreateApplicationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateApplicationUseCaseFacadeImplTest {

    @Mock
    private HandlingCreateApplicationPort handlingCreateApplicationPort;

    private CreateApplicationUseCaseFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new CreateApplicationUseCaseFacadeImpl(handlingCreateApplicationPort);
    }

    @Test
    void execute_delegatesApplicationDto() {
        CreateApplicationDTO dto = new CreateApplicationDTO();

        facade.execute(dto);

        verify(handlingCreateApplicationPort).createApplication(dto);
    }
}
