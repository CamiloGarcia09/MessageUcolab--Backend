package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.catalog.ApplicationCatalogItemDTO;
import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.WorkspaceUserDTO;
import co.edu.uco.application.primaryports.facade.workspace.PersonalWorkspaceUseCaseFacade;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.infraestructure.primaryadapters.PersonalWorkspaceController;
import co.edu.uco.infraestructure.secondaryadapters.presenter.rest.HttpPresenterAdapter;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json.JsonSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.WORKSPACE_USER_ATTRIBUTE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalWorkspaceApplicationsHttpTest {
    private static final String ORGANIZATION_ID = "123e4567-e89b-12d3-a456-426614174001";
    private static final WorkspaceUserDTO USER = new WorkspaceUserDTO(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174002"), "owner@example.test", "Owner");

    @Mock private PersonalWorkspaceUseCaseFacade workspace;
    @Mock private LoggingPortFactory loggerFactory;
    @Mock private LoggingPort logger;
    @Mock private CatalogPort catalog;
    private AnnotationConfigApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(SerializerRegistry.class)).thenReturn(logger);
        when(loggerFactory.getLogger(HttpPresenterAdapter.class)).thenReturn(logger);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(PersonalWorkspaceUseCaseFacade.class, () -> workspace);
        context.registerBean(LoggingPortFactory.class, () -> loggerFactory);
        context.registerBean(CatalogPort.class, () -> catalog);
        context.registerBean(SerializerRegistry.class,
                () -> new SerializerRegistry(List.of(new JsonSerializer()), catalog, loggerFactory));
        context.registerBean(HttpPresenterAdapter.class,
                () -> new HttpPresenterAdapter<>(context.getBean(SerializerRegistry.class), catalog, loggerFactory));
        context.registerBean(PersonalWorkspaceController.class, () -> new PersonalWorkspaceControllerImpl(
                workspace, context.getBean(HttpPresenterAdapter.class), context.getBean(HttpPresenterAdapter.class)));
        context.registerBean(TestWebConfiguration.class);
        context.refresh();
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void applicationsEndpoint_serializesSelectedLanguageWithinEnvelopeData() {
        when(workspace.findApplications(ORGANIZATION_ID, USER)).thenReturn(List.of(
                new ApplicationCatalogItemDTO("app-en", "English selected", CatalogItemDTO.create("lang-en", "English")),
                new ApplicationCatalogItemDTO("app-no-language", "No language", null)));

        client.get().uri("/messageucolab/v1/organizations/{organizationId}/applications", ORGANIZATION_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data[0].id").isEqualTo("app-en")
                .jsonPath("$.data[0].language.id").isEqualTo("lang-en")
                .jsonPath("$.data[0].language.name").isEqualTo("English")
                .jsonPath("$.data[1].language").doesNotExist();

        verify(workspace).findApplications(ORGANIZATION_ID, USER);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    static class TestWebConfiguration {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        WebFilter localTestSessionFixture() {
            return (ServerWebExchange exchange, WebFilterChain chain) -> {
                exchange.getAttributes().put(WORKSPACE_USER_ATTRIBUTE, USER);
                return chain.filter(exchange);
            };
        }
    }
}
