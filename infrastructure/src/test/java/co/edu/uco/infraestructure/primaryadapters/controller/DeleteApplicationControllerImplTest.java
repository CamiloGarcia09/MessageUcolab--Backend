package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.application.DeleteApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationControllerImplTest {
    @Mock DeleteApplicationUseCaseFacade facade;
    @Mock CatalogPort catalog;
    @Mock PresenterPort<String> presenter;

    @Test
    void deleteApplication_usesExchangeIdentityAndPathUuidOnly() {
        var applicationId = UUID.randomUUID().toString();
        var user = new WorkspaceUserDTO(UUID.randomUUID(), "owner@test", "Owner");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.delete("/v1/application/" + applicationId)
                .header("X-Application-Id", "attacker-value").build());
        exchange.getAttributes().put(WORKSPACE_USER_ATTRIBUTE, user);
        when(catalog.getMessage("FUN_230")).thenReturn("deleted");
        when(presenter.presentRestSuccess(anyList(), any())).thenReturn(new RenderedResponse("{}", "application/json"));

        StepVerifier.create(new DeleteApplicationControllerImpl(facade, catalog, presenter)
                .deleteApplication(applicationId, exchange)).verifyComplete();

        verify(facade).execute(applicationId, user);
        verify(presenter).presentRestSuccess(List.of("deleted"), null);
        verifyNoMoreInteractions(facade);
    }
}
