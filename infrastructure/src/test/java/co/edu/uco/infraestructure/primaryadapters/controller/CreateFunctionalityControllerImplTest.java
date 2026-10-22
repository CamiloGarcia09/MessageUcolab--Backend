package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.functionality.CreateFunctionalityDTO;
import co.edu.uco.application.primaryports.facade.functionality.CreateFunctionalityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.impl.SessionApplicationUseCaseFacadeImpl;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.secondaryports.security.SecurityApplicationPort;
import co.edu.uco.application.usecase.SessionApplicationUseCase;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationRegistrationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFunctionalityControllerImplTest {
    private static final String APP_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";

    @Mock private CreateFunctionalityUseCaseFacade createFacade;
    @Mock private PresenterPort<String> presenter;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private EnvironmentRepository environmentRepository;
    @Mock private SecurityApplicationPort securityPort;
    @Mock private CatalogPort catalog;

    private CreateFunctionalityControllerImpl controller;

    @BeforeEach
    void setUp() {
        SessionApplicationUseCase useCase = new SessionApplicationUseCase(securityPort, applicationRepository,
                new SessionApplicationCompositeValidator(catalog, applicationRepository, environmentRepository),
                new SessionApplicationAccessCompositeValidator(catalog, applicationRepository),
                new SessionApplicationRegistrationCompositeValidator(catalog), catalog);
        controller = new CreateFunctionalityControllerImpl(createFacade,
                new SessionApplicationUseCaseFacadeImpl(useCase), presenter);
    }

    @Test
    void createFunctionality_rejectsNullSessionApplicationsBeforeExecutingCreate() {
        assertRejected(null);
    }

    @Test
    void createFunctionality_rejectsEmptySessionApplicationsBeforeExecutingCreate() {
        assertRejected(List.of());
    }

    @Test
    void createFunctionality_rejectsUnassignedApplicationBeforeExecutingCreate() {
        assertRejected(List.of(CatalogItemDTO.create(OTHER_ID, "Other")));
    }

    private void assertRejected(List<CatalogItemDTO> applications) {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder()
                .applicationId(APP_ID).name("Search messages").stateId("state-1").build();
        when(catalog.getMessage("FUN_153")).thenReturn("Application is not assigned");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/messageucolab/v1/functionality").build());
        if (applications != null) {
            exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, applications);
        }

        StepVerifier.create(controller.createFunctionality(dto, exchange))
                .expectError(ForbiddenException.class)
                .verify();

        verify(createFacade, never()).execute(any());
        verify(presenter, never()).presentRestSuccess(anyList(), any());
        verifyNoInteractions(applicationRepository, environmentRepository, securityPort);
    }
}
