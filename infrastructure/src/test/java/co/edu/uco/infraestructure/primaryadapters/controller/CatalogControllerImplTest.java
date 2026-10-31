package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.facade.catalog.FindCatalogUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
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

import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogControllerImpl")
class CatalogControllerImplTest {

    private static final List<CatalogItemDTO> SESSION_APPLICATIONS =
            List.of(CatalogItemDTO.create("app-1", "Aplicacion"));

    @Mock
    private FindCatalogUseCaseFacade facade;
    @Mock
    private PresenterPort<CatalogItemDTO> presenter;

    private CatalogControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogControllerImpl(facade, presenter);
    }

    private MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/messageucolab/v1/catalog/applications").build());
        exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, SESSION_APPLICATIONS);
        return exchange;
    }

    private void stubRender() {
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    @Nested
    @DisplayName("getApplications")
    class GetApplications {

        @Test
        @DisplayName("Delega con las aplicaciones asignadas en sesión y presenta el resultado")
        void delegatesWithSessionApplicationsAndPresents() {
            List<CatalogItemDTO> applications = List.of(CatalogItemDTO.create("app-1", "Aplicacion"));
            stubRender();
            when(facade.findApplications(SESSION_APPLICATIONS)).thenReturn(applications);

            StepVerifier.create(controller.getApplications(exchange())).verifyComplete();

            verify(facade).findApplications(SESSION_APPLICATIONS);
            verify(presenter).presentRestSuccess(applications, null);
        }

        @Test
        @DisplayName("Presenta una lista vacia sin fallar")
        void presentsEmptyCatalog() {
            stubRender();
            when(facade.findApplications(SESSION_APPLICATIONS)).thenReturn(List.of());

            StepVerifier.create(controller.getApplications(exchange())).verifyComplete();

            verify(presenter).presentRestSuccess(List.of(), null);
        }

        @Test
        @DisplayName("Propaga el error del caso de uso sin presentar")
        void propagatesFacadeFailure() {
            BusinessException failure = BusinessException.buildUserException("fallo de catalogo");
            when(facade.findApplications(SESSION_APPLICATIONS)).thenThrow(failure);

            StepVerifier.create(controller.getApplications(exchange()))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();

            verify(presenter, never()).presentRestSuccess(anyList(), any());
        }
    }

    @Nested
    @DisplayName("getEnvironmentsByApplication")
    class GetEnvironmentsByApplication {

        @Test
        @DisplayName("Delega con el id de aplicacion y la identidad")
        void delegatesWithApplicationIdAndIdentity() {
            List<CatalogItemDTO> environments = List.of(CatalogItemDTO.create("env-1", "Produccion"));
            stubRender();
            when(facade.findEnvironmentsByApplication("app-1", SESSION_APPLICATIONS)).thenReturn(environments);

            StepVerifier.create(controller.getEnvironmentsByApplication("app-1", exchange())).verifyComplete();

            verify(facade).findEnvironmentsByApplication("app-1", SESSION_APPLICATIONS);
            verify(presenter).presentRestSuccess(environments, null);
        }
    }

    @Nested
    @DisplayName("getFunctionalitiesByApplication")
    class GetFunctionalitiesByApplication {

        @Test
        @DisplayName("Delega con el id de aplicacion y la identidad")
        void delegatesWithApplicationIdAndIdentity() {
            List<CatalogItemDTO> functionalities = List.of(CatalogItemDTO.create("fun-1", "Envio de correo"));
            stubRender();
            when(facade.findFunctionalitiesByApplication("app-1", SESSION_APPLICATIONS)).thenReturn(functionalities);

            StepVerifier.create(controller.getFunctionalitiesByApplication("app-1", exchange())).verifyComplete();

            verify(facade).findFunctionalitiesByApplication("app-1", SESSION_APPLICATIONS);
            verify(presenter).presentRestSuccess(functionalities, null);
        }
    }

    @Nested
    @DisplayName("catalogos sin identidad")
    class StatelessCatalogs {

        @Test
        @DisplayName("getMessageTypes delega y presenta el resultado")
        void getMessageTypesDelegatesAndPresents() {
            List<CatalogItemDTO> types = List.of(CatalogItemDTO.create("type-1", "Informativo"));
            stubRender();
            when(facade.findMessageTypes()).thenReturn(types);

            StepVerifier.create(controller.getMessageTypes(exchange())).verifyComplete();

            verify(facade).findMessageTypes();
            verify(presenter).presentRestSuccess(types, null);
        }

        @Test
        @DisplayName("getMessageCategories delega y presenta el resultado")
        void getMessageCategoriesDelegatesAndPresents() {
            List<CatalogItemDTO> categories = List.of(CatalogItemDTO.create("cat-1", "Seguridad"));
            stubRender();
            when(facade.findMessageCategories()).thenReturn(categories);

            StepVerifier.create(controller.getMessageCategories(exchange())).verifyComplete();

            verify(facade).findMessageCategories();
            verify(presenter).presentRestSuccess(categories, null);
        }

        @Test
        @DisplayName("getMessageStates delega y presenta el resultado")
        void getMessageStatesDelegatesAndPresents() {
            List<CatalogItemDTO> states = List.of(CatalogItemDTO.create("state-1", "Activo"));
            stubRender();
            when(facade.findMessageStates()).thenReturn(states);

            StepVerifier.create(controller.getMessageStates(exchange())).verifyComplete();

            verify(facade).findMessageStates();
            verify(presenter).presentRestSuccess(states, null);
        }

        @Test
        @DisplayName("getMessageEnvironmentStates delega y presenta el resultado")
        void getMessageEnvironmentStatesDelegatesAndPresents() {
            List<CatalogItemDTO> states = List.of(CatalogItemDTO.create("envstate-1", "Habilitado"));
            stubRender();
            when(facade.findMessageEnvironmentStates()).thenReturn(states);

            StepVerifier.create(controller.getMessageEnvironmentStates(exchange())).verifyComplete();

            verify(facade).findMessageEnvironmentStates();
            verify(presenter).presentRestSuccess(states, null);
        }
    }
}
