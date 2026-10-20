package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnvironmentContextResolver")
class EnvironmentContextResolverTest {

    private static final ExternalIdentity IDENTITY =
            new ExternalIdentity("issuer", "subject", "user@uco.edu.co", Instant.MAX);

    @Mock
    private ActiveContextUseCaseFacade activeContext;
    @Mock
    private CatalogPort catalog;

    private EnvironmentContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EnvironmentContextResolver(activeContext, catalog);
    }

    @Test
    @DisplayName("Devuelve el ambiente activo resuelto para la identidad externa")
    void resolveExternal_returnsEnvironmentForIdentity() {
        when(activeContext.findActiveContext(IDENTITY))
                .thenReturn(ActiveContextDTO.builder().environmentId("env-123").build());

        assertThat(resolver.resolveExternal(IDENTITY)).isEqualTo("env-123");

        verify(activeContext).findActiveContext(IDENTITY);
    }

    @Test
    @DisplayName("Rechaza contexto inexistente o sin ambiente")
    void resolveExternal_withoutEnvironment_throwsUnauthorized() {
        when(activeContext.findActiveContext(IDENTITY)).thenReturn(null);
        when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> resolver.resolveExternal(IDENTITY));

        assertThat(exception.getUserMessage()).isEqualTo("ambiente requerido");
    }

    @Test
    @DisplayName("Propaga denegaciones y ausencia de autorización específicas")
    void resolveExternal_propagatesForbiddenException() {
        ForbiddenException denied = ForbiddenException.buildUserException("denegado");
        when(activeContext.findActiveContext(IDENTITY)).thenThrow(denied);

        assertThat(assertThrows(ForbiddenException.class, () -> resolver.resolveExternal(IDENTITY)))
                .isSameAs(denied);
    }

    @Test
    @DisplayName("Propaga una ausencia de contexto como NotFoundException")
    void resolveExternal_propagatesNotFoundException() {
        NotFoundException missing = NotFoundException.buildUserException("no encontrado");
        when(activeContext.findActiveContext(IDENTITY)).thenThrow(missing);

        assertThat(assertThrows(NotFoundException.class, () -> resolver.resolveExternal(IDENTITY)))
                .isSameAs(missing);
    }
}
