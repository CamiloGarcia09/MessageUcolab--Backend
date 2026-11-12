package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.authorization.ConsumerAuthorizationPort;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.application.usecase.domain.security.ConsumerMessageAccess;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumerMessageAccessResolver")
class ConsumerMessageAccessResolverTest {

    @Mock private ServiceIdentityRepository identities;
    @Mock private ConsumerAuthorizationPort authorization;
    @Mock private CatalogPort catalog;
    private ConsumerMessageAccessResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(catalog.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        resolver = new ConsumerMessageAccessResolver(identities, authorization, catalog);
    }

    @Test
    @DisplayName("Deduce la aplicación y el ambiente del client_id asociado")
    void resolvesAssociatedIdentity() {
        when(identities.findByClientId("inventario-dev")).thenReturn(Optional.of(
                new ServiceIdentityData("inventario-dev", "app-1", "env-1", "develop", "")));
        when(authorization.canConsume("app-1", "env-1")).thenReturn(true);

        assertThat(resolver.resolve(" inventario-dev ")).isEqualTo(new ConsumerMessageAccess("app-1", "env-1"));
    }

    @Test
    @DisplayName("Rechaza una identidad que no está asociada a ningún ambiente")
    void rejectsUnregisteredIdentity() {
        when(identities.findByClientId("inventario-dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("inventario-dev"))
                .isInstanceOf(ForbiddenException.class).hasFieldOrPropertyWithValue("userMessage", "FUN_232");
        verifyNoInteractions(authorization);
    }

    @Test
    @DisplayName("Rechaza sin consultar cuando el token no trae un client_id válido")
    void rejectsMissingOrInvalidClientId() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> resolver.resolve("con espacio")).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(identities, authorization);
    }

    @Test
    @DisplayName("Respeta el puerto de autorización adicional")
    void rejectsWhenAuthorizationPortDenies() {
        when(identities.findByClientId("inventario-dev")).thenReturn(Optional.of(
                new ServiceIdentityData("inventario-dev", "app-1", "env-1", "develop", "")));
        when(authorization.canConsume("app-1", "env-1")).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolve("inventario-dev"))
                .isInstanceOf(ForbiddenException.class).hasFieldOrPropertyWithValue("userMessage", "FUN_221");
    }
}
