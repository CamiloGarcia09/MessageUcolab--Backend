package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import co.edu.uco.application.primaryports.facade.functionality.UpdateFunctionalityStateUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFunctionalityStateControllerImplTest {
    private static final String FUNCTIONALITY_ID = "123e4567-e89b-12d3-a456-426614175801";
    private static final String REQUESTED_APPLICATION_ID = "123e4567-e89b-12d3-a456-426614175802";
    private static final String RESOLVED_APPLICATION_ID = "123e4567-e89b-12d3-a456-426614175803";
    private static final String STATE_ID = "123e4567-e89b-12d3-a456-426614175804";
    private static final List<CatalogItemDTO> APPLICATIONS = List.of(
            CatalogItemDTO.create(REQUESTED_APPLICATION_ID, "Owned application"));

    @Mock private UpdateFunctionalityStateUseCaseFacade facade;
    @Mock private SessionApplicationUseCaseFacade session;
    @Mock private PresenterPort<String> presenter;
    @Mock private CatalogPort catalog;
    private UpdateFunctionalityStateControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new UpdateFunctionalityStateControllerImpl(facade, session, presenter, catalog);
    }

    @Test
    void updateState_resolvesOwnedApplicationBeforeMutatingAndPresentsCatalogConfirmation() {
        UpdateFunctionalityStateDTO request = new UpdateFunctionalityStateDTO(REQUESTED_APPLICATION_ID, STATE_ID);
        UpdateFunctionalityStateDTO resolved = new UpdateFunctionalityStateDTO(RESOLVED_APPLICATION_ID, STATE_ID);
        MockServerWebExchange exchange = exchangeWithApplications(APPLICATIONS);
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APPLICATION_ID)).thenReturn(RESOLVED_APPLICATION_ID);
        when(catalog.getMessage(MessageCatalogCodeEnum.FUN_227.getCode())).thenReturn("Functionality state updated");
        when(presenter.presentRestSuccess(List.of("Functionality state updated"), "application/json"))
                .thenReturn(new RenderedResponse("{\"message\":\"Functionality state updated\"}", "application/json"));

        StepVerifier.create(controller.updateState(FUNCTIONALITY_ID, request, exchange)).verifyComplete();

        InOrder order = inOrder(session, facade, catalog, presenter);
        order.verify(session).resolveApplication(APPLICATIONS, REQUESTED_APPLICATION_ID);
        order.verify(facade).execute(FUNCTIONALITY_ID, resolved);
        order.verify(catalog).getMessage(MessageCatalogCodeEnum.FUN_227.getCode());
        order.verify(presenter).presentRestSuccess(List.of("Functionality state updated"), "application/json");
    }

    @Test
    void updateState_rejectsApplicationNotOwnedBySessionWithoutFacadeOrConfirmation() {
        MockServerWebExchange exchange = exchangeWithApplications(APPLICATIONS);
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APPLICATION_ID))
                .thenThrow(ForbiddenException.buildUserException("Application is not assigned to this session"));

        StepVerifier.create(controller.updateState(FUNCTIONALITY_ID,
                        new UpdateFunctionalityStateDTO(REQUESTED_APPLICATION_ID, STATE_ID), exchange))
                .expectError(ForbiddenException.class)
                .verify();

        verify(session).resolveApplication(APPLICATIONS, REQUESTED_APPLICATION_ID);
        verify(facade, never()).execute(any(), any());
        verifyNoInteractions(catalog, presenter);
    }

    @Test
    void updateState_rejectsNullBodyBeforeAnyMutation() {
        MockServerWebExchange exchange = exchangeWithApplications(APPLICATIONS);
        when(session.resolveApplication(APPLICATIONS, null))
                .thenThrow(ForbiddenException.buildUserException("Application is required"));

        StepVerifier.create(controller.updateState(FUNCTIONALITY_ID, null, exchange))
                .expectError(ForbiddenException.class)
                .verify();

        verify(session).resolveApplication(APPLICATIONS, null);
        verify(facade, never()).execute(any(), any());
        verifyNoInteractions(catalog, presenter);
    }

    @Test
    void updateState_rejectsMissingSessionApplicationsWithoutCallingFacade() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.patch("/messageucolab/v1/application/functionality/{id}/state", FUNCTIONALITY_ID)
                        .header("Accept", "application/json").build());
        when(session.resolveApplication(null, REQUESTED_APPLICATION_ID))
                .thenThrow(ForbiddenException.buildUserException("No applications in session"));

        StepVerifier.create(controller.updateState(FUNCTIONALITY_ID,
                        new UpdateFunctionalityStateDTO(REQUESTED_APPLICATION_ID, STATE_ID), exchange))
                .expectError(ForbiddenException.class)
                .verify();

        verify(session).resolveApplication(null, REQUESTED_APPLICATION_ID);
        verify(facade, never()).execute(any(), any());
        verifyNoInteractions(catalog, presenter);
    }

    private static MockServerWebExchange exchangeWithApplications(List<CatalogItemDTO> applications) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.patch("/messageucolab/v1/application/functionality/{id}/state", FUNCTIONALITY_ID)
                        .header("Accept", "application/json").build());
        exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, applications);
        return exchange;
    }
}
