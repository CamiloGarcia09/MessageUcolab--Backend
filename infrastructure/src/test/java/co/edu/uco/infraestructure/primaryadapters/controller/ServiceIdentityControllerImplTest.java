package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.ServiceIdentityDTO;
import co.edu.uco.application.primaryports.facade.serviceidentity.ServiceIdentityUseCaseFacade;
import co.edu.uco.application.primaryports.facade.session.SessionApplicationUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json.JsonSerializer;
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

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_236;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_238;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.SESSION_APPLICATIONS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceIdentityControllerImpl")
class ServiceIdentityControllerImplTest {

    private static final String REQUESTED_APP = "123e4567-e89b-12d3-a456-426614175802";
    private static final String OWNED_APP = "123e4567-e89b-12d3-a456-426614175803";
    private static final String ENVIRONMENT = "123e4567-e89b-12d3-a456-426614175804";
    private static final List<CatalogItemDTO> APPLICATIONS = List.of(CatalogItemDTO.create(REQUESTED_APP, "App"));

    @Mock private ServiceIdentityUseCaseFacade facade;
    @Mock private SessionApplicationUseCaseFacade session;
    @Mock private PresenterPort<String> presenter;
    @Mock private SerializerRegistry serializers;
    @Mock private CatalogPort catalog;
    private ServiceIdentityControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceIdentityControllerImpl(facade, session, presenter, serializers, catalog);
    }

    @Test
    @DisplayName("Lista las identidades de la aplicación propia como JSON sin caché")
    void listsIdentitiesOfOwnedApplication() {
        var exchange = exchange();
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APP)).thenReturn(OWNED_APP);
        when(facade.list(OWNED_APP)).thenReturn(List.of(
                new ServiceIdentityDTO("inventario-dev", ENVIRONMENT, "develop", "2026-11-10T20:00:00Z")));
        when(serializers.getSerializerForMediaType("application/json")).thenReturn(new JsonSerializer());

        StepVerifier.create(controller.list(REQUESTED_APP, exchange)).verifyComplete();

        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"clientId\":\"inventario-dev\"").contains("\"environmentName\":\"develop\"");
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    @DisplayName("Registra la identidad en la aplicación propia y confirma con el catálogo")
    void registersIdentity() {
        var exchange = exchange();
        var request = new RegisterServiceIdentityDTO(ENVIRONMENT, "inventario-dev");
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APP)).thenReturn(OWNED_APP);
        when(catalog.getMessage(FUN_236.getCode())).thenReturn("Identidad registrada");
        when(presenter.presentRestSuccess(List.of("Identidad registrada"), "application/json"))
                .thenReturn(new RenderedResponse("{\"data\":[\"Identidad registrada\"]}", "application/json"));

        StepVerifier.create(controller.register(REQUESTED_APP, request, exchange)).verifyComplete();

        verify(facade).register(OWNED_APP, request);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("Identidad registrada");
    }

    @Test
    @DisplayName("Retira la identidad de la aplicación propia y confirma con el catálogo")
    void removesIdentity() {
        var exchange = exchange();
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APP)).thenReturn(OWNED_APP);
        when(catalog.getMessage(FUN_238.getCode())).thenReturn("Identidad retirada");
        when(presenter.presentRestSuccess(List.of("Identidad retirada"), "application/json"))
                .thenReturn(new RenderedResponse("{\"data\":[\"Identidad retirada\"]}", "application/json"));

        StepVerifier.create(controller.remove(REQUESTED_APP, "inventario-dev", exchange)).verifyComplete();

        verify(facade).remove(OWNED_APP, "inventario-dev");
    }

    @Test
    @DisplayName("Rechaza una aplicación ajena sin tocar las identidades")
    void rejectsForeignApplication() {
        var exchange = exchange();
        when(session.resolveApplication(APPLICATIONS, REQUESTED_APP))
                .thenThrow(ForbiddenException.buildUserException("Aplicación ajena"));

        StepVerifier.create(controller.register(REQUESTED_APP, new RegisterServiceIdentityDTO(ENVIRONMENT, "x"), exchange))
                .expectError(ForbiddenException.class).verify();
        StepVerifier.create(controller.remove(REQUESTED_APP, "x", exchange))
                .expectError(ForbiddenException.class).verify();
        StepVerifier.create(controller.list(REQUESTED_APP, exchange))
                .expectError(ForbiddenException.class).verify();

        verify(facade, never()).register(anyString(), any());
        verify(facade, never()).remove(anyString(), anyString());
        verify(facade, never()).list(anyString());
        verifyNoInteractions(catalog, presenter, serializers);
    }

    private static MockServerWebExchange exchange() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/messageucolab/v1/application/{id}/service-identities", REQUESTED_APP)
                .header("Accept", "application/json").build());
        exchange.getAttributes().put(SESSION_APPLICATIONS_ATTRIBUTE, APPLICATIONS);
        return exchange;
    }
}
