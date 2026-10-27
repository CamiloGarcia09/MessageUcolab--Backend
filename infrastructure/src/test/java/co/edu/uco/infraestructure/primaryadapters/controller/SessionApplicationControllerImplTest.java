package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionApplicationControllerImpl")
class SessionApplicationControllerImplTest {

    @Mock
    private SessionApplicationUseCaseFacade facade;
    @Mock
    private PresenterPort<SessionApplicationDTO> presenter;

    private SessionApplicationControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new SessionApplicationControllerImpl(facade, presenter);
    }

    @Test
    @DisplayName("Presents the applications previously authorized by the session interceptor")
    void getSessionApplications_usesInterceptorApplicationsAndPresentsResult() {
        List<CatalogItemDTO> sessionApplications = List.of(CatalogItemDTO.create("app-1", "Application"));
        List<SessionApplicationDTO> result = List.of();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/session/applications").build());
        exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, sessionApplications);
        when(facade.findSessionApplications(sessionApplications)).thenReturn(result);
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));

        StepVerifier.create(controller.getSessionApplications(exchange)).verifyComplete();

        verify(facade).findSessionApplications(sessionApplications);
        verify(presenter).presentRestSuccess(result, null);
    }

    @Test
    @DisplayName("Propagates a domain authorization failure")
    void getSessionApplications_propagatesForbiddenFailure() {
        List<CatalogItemDTO> sessionApplications = List.of();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/session/applications").build());
        exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, sessionApplications);
        ForbiddenException failure = ForbiddenException.buildUserException("Access denied");
        when(facade.findSessionApplications(sessionApplications)).thenThrow(failure);

        StepVerifier.create(controller.getSessionApplications(exchange))
                .verifyErrorSatisfies(error -> org.assertj.core.api.Assertions.assertThat(error).isSameAs(failure));

        verify(facade).findSessionApplications(sessionApplications);
        verifyNoInteractions(presenter);
    }
}
