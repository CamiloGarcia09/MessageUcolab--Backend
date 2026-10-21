package co.edu.uco.application.usecase.security;

import co.edu.uco.application.primaryports.dto.context.ActiveContextDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.domain.security.AuthorizationScopeType;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.handling.HandlingActiveContextPort;
import co.edu.uco.application.usecase.validator.authorization.AuthorizationCompositeValidator;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageEnvironmentResolverImplTest {

    private static final UUID ENVIRONMENT_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174003");
    private static final ExternalIdentity IDENTITY = new ExternalIdentity(
            "issuer", "subject", "user@example.com", Instant.MAX);

    private final HandlingActiveContextPort activeContextPort = mock(HandlingActiveContextPort.class);
    private final AuthorizationCompositeValidator authorizationCompositeValidator =
            mock(AuthorizationCompositeValidator.class);
    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    private final CatalogPort catalogPort = mock(CatalogPort.class);
    private final MessageEnvironmentResolver resolver = new MessageEnvironmentResolverImpl(
            activeContextPort, authorizationCompositeValidator, environmentRepository, catalogPort);
    private static final UUID APPLICATION_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174020");

    @Test
    void resolveApplication_returnsNormalizedSessionApplicationWithoutFurtherLookups() {
        MessageAccessContext context = new MessageAccessContext(null, null, APPLICATION_ID.toString().toUpperCase());

        String result = resolver.resolveApplication(context, PermissionCode.MESSAGE_READ);

        assertThat(result).isEqualTo(APPLICATION_ID.toString());
        verifyNoInteractions(activeContextPort, authorizationCompositeValidator, environmentRepository, catalogPort);
    }

    @Test
    void resolveApplication_authorizesActiveContextApplicationWhenIdentityIsPresent() {
        MessageAccessContext context = new MessageAccessContext(null, IDENTITY);
        when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(ActiveContextDTO.builder()
                .applicationId(APPLICATION_ID.toString())
                .environmentId(ENVIRONMENT_ID.toString())
                .build());

        String result = resolver.resolveApplication(context, PermissionCode.MESSAGE_CREATE);

        assertThat(result).isEqualTo(APPLICATION_ID.toString());
        verify(authorizationCompositeValidator).validate(IDENTITY, PermissionCode.MESSAGE_CREATE,
                AuthorizationScopeType.APPLICATION, APPLICATION_ID);
    }

    @Test
    void resolveApplication_derivesApplicationFromWorkspaceEnvironment() {
        MessageAccessContext context = new MessageAccessContext(ENVIRONMENT_ID.toString(), null);
        when(environmentRepository.findById(ENVIRONMENT_ID.toString())).thenReturn(Optional.of(new EnvironmentData(
                ENVIRONMENT_ID, ApplicationData.build(APPLICATION_ID, "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"))));

        assertThat(resolver.resolveApplication(context, PermissionCode.MESSAGE_READ))
                .isEqualTo(APPLICATION_ID.toString());
    }

    @Test
    void resolveApplication_throwsForbidden_whenWorkspaceEnvironmentDoesNotExist() {
        MessageAccessContext context = new MessageAccessContext(ENVIRONMENT_ID.toString(), null);
        when(environmentRepository.findById(ENVIRONMENT_ID.toString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveApplication(context, PermissionCode.MESSAGE_READ))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void resolveApplication_throwsUnauthorized_whenContextHasNoScope() {
        assertThatThrownBy(() -> resolver.resolveApplication(new MessageAccessContext(null, null),
                PermissionCode.MESSAGE_READ)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> resolver.resolveApplication(null, PermissionCode.MESSAGE_READ))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resolve_returnsLegacyEnvironmentWithoutExternalAuthorization() {
        MessageAccessContext context = new MessageAccessContext("workspace-environment", null);

        String result = resolver.resolve(context, PermissionCode.MESSAGE_READ);

        assertThat(result).isEqualTo("workspace-environment");
        verifyNoInteractions(activeContextPort, authorizationCompositeValidator, catalogPort);
    }

    @Test
    void resolve_usesActiveContextAndOperationPermissionWhenIdentityIsPresent() {
        MessageAccessContext context = new MessageAccessContext("ignored-workspace", IDENTITY);
        when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(
                ActiveContextDTO.builder().environmentId(ENVIRONMENT_ID.toString()).build());

        String result = resolver.resolve(context, PermissionCode.MESSAGE_TRANSLATE);

        assertThat(result).isEqualTo(ENVIRONMENT_ID.toString());
        verify(authorizationCompositeValidator).validate(IDENTITY, PermissionCode.MESSAGE_TRANSLATE,
                AuthorizationScopeType.ENVIRONMENT, ENVIRONMENT_ID);
    }

    @Test
    void resolve_rejectsRequestWithoutExternalIdentityOrLegacyEnvironment() {
        when(catalogPort.getMessage("FUN_152")).thenReturn("Authentication required");
        MessageAccessContext context = new MessageAccessContext(" ", null);

        assertThatThrownBy(() -> resolver.resolve(context, PermissionCode.MESSAGE_READ))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("httpStatus", "userMessage")
                .containsExactly(401, "Authentication required");
        verifyNoInteractions(activeContextPort, authorizationCompositeValidator);
    }

    @Test
    void resolve_propagatesDeniedOperationPermission() {
        MessageAccessContext context = new MessageAccessContext(null, IDENTITY);
        ForbiddenException failure = ForbiddenException.buildUserException("Permission denied");
        when(activeContextPort.findActiveContext(IDENTITY)).thenReturn(
                ActiveContextDTO.builder().environmentId(ENVIRONMENT_ID.toString()).build());
        doThrow(failure).when(authorizationCompositeValidator).validate(IDENTITY, PermissionCode.MESSAGE_READ,
                AuthorizationScopeType.ENVIRONMENT, ENVIRONMENT_ID);

        assertThatThrownBy(() -> resolver.resolve(context, PermissionCode.MESSAGE_READ)).isSameAs(failure);
    }

    @Test
    void resolve_throwsUnauthorized_whenTheContextIsNull() {
        when(catalogPort.getMessage("FUN_152")).thenReturn("Authentication required");

        assertThatThrownBy(() -> resolver.resolve(null, PermissionCode.MESSAGE_READ))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("httpStatus", "userMessage")
                .containsExactly(401, "Authentication required");
        verifyNoInteractions(activeContextPort, authorizationCompositeValidator);
    }
}
