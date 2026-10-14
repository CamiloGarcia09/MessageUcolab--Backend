package co.edu.uco.infraestructure.primaryadapters.interceptors;

import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.primaryports.facade.context.ActiveContextUseCaseFacade;
import co.edu.uco.application.primaryports.facade.token.FindEnvironmentIdTokenUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    private FindEnvironmentIdTokenUseCaseFacade legacyEnvironment;
    @Mock
    private ActiveContextUseCaseFacade activeContext;
    @Mock
    private CatalogPort catalog;

    private EnvironmentContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EnvironmentContextResolver(legacyEnvironment, activeContext, catalog);
    }

    @Nested
    @DisplayName("resolveLegacy")
    class ResolveLegacy {

        @Test
        @DisplayName("Devuelve el ambiente resuelto por el token")
        void returnsEnvironmentIdFromToken() {
            when(legacyEnvironment.execute("token-abc")).thenReturn("env-123");

            assertThat(resolver.resolveLegacy("token-abc")).isEqualTo("env-123");
            verify(legacyEnvironment).execute("token-abc");
        }

        @Test
        @DisplayName("Lanza UnauthorizedException cuando el ambiente es nulo")
        void nullEnvironment_throwsUnauthorized() {
            when(legacyEnvironment.execute("token-abc")).thenReturn(null);
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> resolver.resolveLegacy("token-abc"));

            assertThat(exception.getUserMessage()).isEqualTo("ambiente requerido");
        }

        @Test
        @DisplayName("Lanza UnauthorizedException cuando el ambiente es vacío")
        void blankEnvironment_throwsUnauthorized() {
            when(legacyEnvironment.execute("token-abc")).thenReturn("  ");
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

            assertThrows(UnauthorizedException.class, () -> resolver.resolveLegacy("token-abc"));
        }

        @Test
        @DisplayName("Propaga las excepciones del puerto sin transformarlas")
        void propagatesPortExceptions() {
            NotFoundException failure = NotFoundException.buildUserException("token inexistente");
            when(legacyEnvironment.execute("token-abc")).thenThrow(failure);

            NotFoundException exception = assertThrows(NotFoundException.class,
                    () -> resolver.resolveLegacy("token-abc"));

            assertThat(exception).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("resolveExternal")
    class ResolveExternal {

        @Test
        @DisplayName("Devuelve el ambiente activo de la identidad externa")
        void returnsActiveEnvironmentForIdentity() {
            when(activeContext.findActiveContext(IDENTITY))
                    .thenReturn(ActiveContextDTO.builder().environmentId("env-active").build());

            assertThat(resolver.resolveExternal(IDENTITY)).isEqualTo("env-active");
            verify(activeContext).findActiveContext(IDENTITY);
        }

        @Test
        @DisplayName("Lanza UnauthorizedException cuando no hay contexto activo")
        void missingContext_throwsUnauthorized() {
            when(activeContext.findActiveContext(IDENTITY)).thenReturn(null);
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> resolver.resolveExternal(IDENTITY));

            assertThat(exception.getUserMessage()).isEqualTo("ambiente requerido");
        }

        @Test
        @DisplayName("Lanza UnauthorizedException cuando el contexto activo no tiene ambiente")
        void contextWithoutEnvironment_throwsUnauthorized() {
            when(activeContext.findActiveContext(IDENTITY))
                    .thenReturn(ActiveContextDTO.builder().organizationId("org-1").build());
            when(catalog.getMessage("FUN_152")).thenReturn("ambiente requerido");

            assertThrows(UnauthorizedException.class, () -> resolver.resolveExternal(IDENTITY));
        }
    }
}