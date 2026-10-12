package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.dto.context.AvailableContextDTO;
import co.edu.uco.application.primaryports.dto.context.SelectActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeContextControllerImpl")
class MeContextControllerImplTest {

    private static final ExternalIdentity IDENTITY = new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);

    @Mock
    private ActiveContextUseCaseFacade facade;
    @Mock
    private PresenterPort<AvailableContextDTO> availablePresenter;
    @Mock
    private PresenterPort<ActiveContextDTO> activePresenter;

    private MeContextControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new MeContextControllerImpl(facade, availablePresenter, activePresenter);
    }

    private MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/me/context").build());
        exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, IDENTITY);
        return exchange;
    }

    @Nested
    @DisplayName("getAvailableContexts")
    class GetAvailableContexts {

        @Test
        @DisplayName("Presenta una lista vacia cuando no hay contextos disponibles")
        void presentsEmptyListWhenNoContexts() {
            when(availablePresenter.presentRestSuccess(anyList(), any()))
                    .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
            when(facade.findAvailableContexts(IDENTITY)).thenReturn(List.of());

            StepVerifier.create(controller.getAvailableContexts(exchange())).verifyComplete();

            verify(availablePresenter).presentRestSuccess(List.of(), null);
        }

        @Test
        @DisplayName("Delega con la identidad y presenta los contextos disponibles")
        void delegatesWithIdentityAndPresents() {
            when(availablePresenter.presentRestSuccess(anyList(), any()))
                    .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
            List<AvailableContextDTO> contexts = List.of(AvailableContextDTO.builder()
                    .organization(CatalogItemDTO.create("org-1", "Organización"))
                    .application(CatalogItemDTO.create("app-1", "Aplicación"))
                    .environment(CatalogItemDTO.create("env-1", "Producción"))
                    .build());
            when(facade.findAvailableContexts(IDENTITY)).thenReturn(contexts);

            StepVerifier.create(controller.getAvailableContexts(exchange())).verifyComplete();

            verify(facade).findAvailableContexts(IDENTITY);
            verify(availablePresenter).presentRestSuccess(contexts, null);
        }

        @Test
        @DisplayName("Propaga el error del caso de uso sin presentar")
        void propagatesFacadeFailure() {
            BusinessException failure = BusinessException.buildUserException("sin contextos");
            when(facade.findAvailableContexts(IDENTITY)).thenThrow(failure);

            StepVerifier.create(controller.getAvailableContexts(exchange()))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();

            verify(availablePresenter, never()).presentRestSuccess(anyList(), any());
        }
    }

    @Nested
    @DisplayName("getActiveContext")
    class GetActiveContext {

        @Test
        @DisplayName("Delega con la identidad y presenta el contexto activo")
        void delegatesWithIdentityAndPresents() {
            when(activePresenter.presentRestSuccess(anyList(), any()))
                    .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
            ActiveContextDTO active = ActiveContextDTO.builder().organizationId("org-1").build();
            when(facade.findActiveContext(IDENTITY)).thenReturn(active);

            StepVerifier.create(controller.getActiveContext(exchange())).verifyComplete();

            verify(facade).findActiveContext(IDENTITY);
            verify(activePresenter).presentRestSuccess(List.of(active), null);
        }
    }

    @Nested
    @DisplayName("selectActiveContext")
    class SelectActiveContext {

        @Test
        @DisplayName("Delega la selección con la identidad y presenta el contexto resultante")
        void delegatesSelectionWithIdentityAndPresents() {
            when(activePresenter.presentRestSuccess(anyList(), any()))
                    .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
            SelectActiveContextDTO selection = SelectActiveContextDTO.builder()
                    .organizationId("org-1").applicationId("app-1").environmentId("env-1").build();
            ActiveContextDTO active = ActiveContextDTO.builder().organizationId("org-1").build();
            when(facade.selectActiveContext(selection, IDENTITY)).thenReturn(active);

            StepVerifier.create(controller.selectActiveContext(selection, exchange())).verifyComplete();

            verify(facade).selectActiveContext(selection, IDENTITY);
            verify(activePresenter).presentRestSuccess(List.of(active), null);
        }
    }
}
