package co.edu.uco.application.primaryports.facade.session.impl;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.usecase.handling.HandlingSessionApplicationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationUseCaseFacadeImplTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final List<CatalogItemDTO> SESSION = List.of(CatalogItemDTO.create(APPLICATION_ID, "MessageUcoLab"));

    @Mock
    private HandlingSessionApplicationPort port;

    private SessionApplicationUseCaseFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new SessionApplicationUseCaseFacadeImpl(port);
    }

    @Test
    void findCurrentUserApplications_delegatesToPort() {
        when(port.findCurrentUserApplications("session")).thenReturn(SESSION);

        assertThat(facade.findCurrentUserApplications("session")).isSameAs(SESSION);
    }

    @Test
    void findSessionApplications_delegatesToPort() {
        List<SessionApplicationDTO> expected = List.of(SessionApplicationDTO.builder().id(APPLICATION_ID).build());
        when(port.findSessionApplications(SESSION)).thenReturn(expected);

        assertThat(facade.findSessionApplications(SESSION)).isSameAs(expected);
    }

    @Test
    void resolveApplication_delegatesToPort() {
        when(port.resolveApplication(SESSION, APPLICATION_ID)).thenReturn(APPLICATION_ID);

        assertThat(facade.resolveApplication(SESSION, APPLICATION_ID)).isEqualTo(APPLICATION_ID);
    }

    @Test
    void resolveEnvironment_delegatesToPort() {
        when(port.resolveEnvironment(SESSION, APPLICATION_ID, ENVIRONMENT_ID)).thenReturn(ENVIRONMENT_ID);

        assertThat(facade.resolveEnvironment(SESSION, APPLICATION_ID, ENVIRONMENT_ID)).isEqualTo(ENVIRONMENT_ID);
    }

    @Test
    void authorizeApplication_delegatesToPort() {
        facade.authorizeApplication(SESSION, APPLICATION_ID);

        verify(port).authorizeApplication(SESSION, APPLICATION_ID);
    }
}
