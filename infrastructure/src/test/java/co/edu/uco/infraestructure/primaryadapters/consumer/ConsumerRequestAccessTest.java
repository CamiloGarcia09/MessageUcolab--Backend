package co.edu.uco.infraestructure.primaryadapters.consumer;

import co.edu.uco.application.usecase.domain.security.ConsumerMessageAccess;
import co.edu.uco.application.usecase.security.ConsumerMessageAccessResolver;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumerRequestAccess")
class ConsumerRequestAccessTest {

    @Mock private ConsumerMessageAccessResolver resolver;
    @Mock private ServiceIdentityClientIdReader reader;

    @Test
    @DisplayName("Con sesión usa la aplicación y el ambiente que fijó el interceptor")
    void usesSessionAttributes() {
        var exchange = exchange();
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, "app-1");
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, "env-1");

        var access = new ConsumerRequestAccess(resolver, reader).resolve(exchange);

        assertThat(access).isEqualTo(new ConsumerMessageAccess("app-1", "env-1"));
        verifyNoInteractions(resolver, reader);
    }

    @Test
    @DisplayName("Sin sesión resuelve la aplicación y el ambiente con el client_id del token")
    void resolvesFromTokenClientId() {
        var exchange = exchange();
        when(reader.read(exchange)).thenReturn("inventario-dev");
        when(resolver.resolve("inventario-dev")).thenReturn(new ConsumerMessageAccess("app-2", "env-2"));

        var access = new ConsumerRequestAccess(resolver, reader).resolve(exchange);

        assertThat(access).isEqualTo(new ConsumerMessageAccess("app-2", "env-2"));
        verify(resolver).resolve("inventario-dev");
    }

    @Test
    @DisplayName("Propaga el rechazo cuando la identidad no está asociada a un ambiente")
    void propagatesRejection() {
        var exchange = exchange();
        when(reader.read(exchange)).thenReturn(null);
        when(resolver.resolve(null)).thenThrow(ForbiddenException.buildUserException("no asociada"));

        assertThatThrownBy(() -> new ConsumerRequestAccess(resolver, reader).resolve(exchange))
                .isInstanceOf(ForbiddenException.class);
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/messageucolab/v1/messages/consumer").build());
    }
}
