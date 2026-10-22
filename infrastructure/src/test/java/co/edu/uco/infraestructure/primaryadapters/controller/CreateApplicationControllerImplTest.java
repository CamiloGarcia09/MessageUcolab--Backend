package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateApplicationControllerImplTest {
    @Mock private PersonalWorkspaceUseCaseFacade createFacade;
    @Mock private PresenterPort<String> presenter;
    @Mock private CatalogPort catalog;

    private CreateApplicationControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new CreateApplicationControllerImpl(createFacade, catalog, presenter);
    }

    @Test
    void createApplication_usesAuthenticatedWorkspaceUserAndReturnsCataloguedSuccess() {
        WorkspaceUserDTO user = new WorkspaceUserDTO(java.util.UUID.randomUUID(), "owner@example.test", "Owner");
        CreateApplicationDTO dto = CreateApplicationDTO.builder().id("client-id").name("App").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/messageucolab/v1/application").build());
        exchange.getAttributes().put(WORKSPACE_USER_ATTRIBUTE, user);
        when(catalog.getMessage("FUN_216")).thenReturn("Application created");
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new co.edu.uco.application.secondaryports.presenter.RenderedResponse("{}", "application/json"));

        StepVerifier.create(controller.createApplication(dto, exchange)).verifyComplete();

        verify(createFacade).createApplication(dto, user);
        verify(presenter).presentRestSuccess(List.of("Application created"), null);
    }
}
