package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.facade.message.CreateMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.presenter.RenderedResponse;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMessageControllerImpl")
class CreateMessageControllerImplTest {

    @Mock
    private CreateMessageUseCaseFacade facade;
    @Mock
    private PresenterPort<String> presenter;

    private CreateMessageControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new CreateMessageControllerImpl(facade, presenter);
    }

    private MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/messageucolab/v1/messages").build());
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, "env-token");
        return exchange;
    }

    private void stubRender() {
        when(presenter.presentRestSuccess(anyList(), any()))
                .thenReturn(new RenderedResponse("{\"data\":[]}", "application/json"));
    }

    @Nested
    @DisplayName("createMessage")
    class CreateMessage {

        @Test
        @DisplayName("Ejecuta el caso de uso y presenta el éxito")
        void executesAndPresents() {
            stubRender();
            CreateMessageDTO dto = CreateMessageDTO.builder().code("MSG-001").build();

            StepVerifier.create(controller.createMessage(dto, exchange())).verifyComplete();

            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(facade).execute(eq(dto), contextCaptor.capture());
            assertAll(
                    () -> assertThat(contextCaptor.getValue().workspaceEnvironmentId()).isEqualTo("env-token"),
                    () -> assertThat(contextCaptor.getValue().externalIdentity()).isNull());
            verify(presenter).presentRestSuccess(List.of("Mensaje creado exitosamente"), null);
        }

        @Test
        @DisplayName("Incluye la identidad externa en el contexto cuando está presente")
        void includesExternalIdentityWhenPresent() {
            stubRender();
            CreateMessageDTO dto = CreateMessageDTO.builder().code("MSG-002").build();
            MockServerWebExchange exchange = exchange();
            ExternalIdentity identity = new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);
            exchange.getAttributes().put(EXTERNAL_IDENTITY_ATTRIBUTE, identity);

            StepVerifier.create(controller.createMessage(dto, exchange)).verifyComplete();

            ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
            verify(facade).execute(eq(dto), contextCaptor.capture());
            assertThat(contextCaptor.getValue().externalIdentity()).isSameAs(identity);
        }

        @Test
        @DisplayName("Propaga el error del caso de uso sin presentar")
        void propagatesFacadeFailureWithoutPresenting() {
            CreateMessageDTO dto = CreateMessageDTO.builder().code("MSG-001").build();
            ForbiddenException failure = ForbiddenException.buildUserException("sin permisos");
            doThrow(failure).when(facade).execute(eq(dto), any(MessageAccessContext.class));

            StepVerifier.create(controller.createMessage(dto, exchange()))
                    .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                    .verify();

            verify(presenter, never()).presentRestSuccess(anyList(), any());
        }
    }
}
