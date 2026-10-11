package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.EXTERNAL_IDENTITY_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveMessageControllerImplTest {
    @Mock private ResolveMessageUseCaseFacade facade;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @Test
    void resolve_returnsMinimalDtoAndSetsNoStore_forExternalIdentityContext() {
        var identity = new co.edu.uco.application.usecase.domain.security.ExternalIdentity("issuer", "subject", "email", null);
        when(request.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).thenReturn("env");
        when(request.getAttribute(EXTERNAL_IDENTITY_ATTRIBUTE)).thenReturn(identity);
        var expected = new ResolvedMessageDTO("CODE", "Title", "Content");
        when(facade.execute(eq("CODE"), any()))
                .thenReturn(expected);
        var controller = new ResolveMessageControllerImpl(facade);

        var actual = controller.resolve("CODE", request, response);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<MessageAccessContext> context = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(facade).execute(eq("CODE"), context.capture());
        assertThat(context.getValue().externalIdentity()).isSameAs(identity);
        assertThat(context.getValue().legacyEnvironmentId()).isEqualTo("env");
        verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    @Test
    void resolve_preservesLegacyEnvironment_whenNoExternalIdentityExists() {
        when(request.getAttribute(ENVIRONMENT_ID_ATTRIBUTE)).thenReturn("legacy-env");
        var expected = new ResolvedMessageDTO("CODE", "Title", "Content");
        when(facade.execute(eq("CODE"), any()))
                .thenReturn(expected);
        var controller = new ResolveMessageControllerImpl(facade);

        controller.resolve("CODE", request, response);

        ArgumentCaptor<MessageAccessContext> context = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(facade).execute(eq("CODE"), context.capture());
        assertThat(context.getValue().legacyEnvironmentId()).isEqualTo("legacy-env");
        assertThat(context.getValue().externalIdentity()).isNull();
    }
}
