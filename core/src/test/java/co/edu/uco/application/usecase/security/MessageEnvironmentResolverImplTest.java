package co.edu.uco.application.usecase.security;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageEnvironmentResolverImplTest {

    private static final String APPLICATION_ID = "123e4567-e89b-12d3-a456-426614175020";
    private static final String ENVIRONMENT_ID = "123e4567-e89b-12d3-a456-426614175000";

    @Mock
    private EnvironmentRepository environmentRepository;
    @Mock
    private CatalogPort catalogPort;

    private MessageEnvironmentResolverImpl resolver;

    @BeforeEach
    void setUp() {
        MessageEnvironmentSelectionCompositeValidator validator =
                new MessageEnvironmentSelectionCompositeValidator(catalogPort, environmentRepository);
        resolver = new MessageEnvironmentResolverImpl(validator, catalogPort);
    }

    @Test
    void resolveApplication_returnsSessionApplicationIdAsCanonicalUuid() {
        MessageAccessContext context = new MessageAccessContext(APPLICATION_ID.toUpperCase());

        String result = resolver.resolveApplication(context);

        assertThat(result).isEqualTo(APPLICATION_ID);
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void resolveApplication_rejectsNullContext() {
        assertThatThrownBy(() -> resolver.resolveApplication(null))
                .isInstanceOf(UnauthorizedException.class)
                .isInstanceOf(UnauthorizedException.class);

        verify(catalogPort).getMessage("FUN_152");
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void resolveApplication_rejectsContextWithoutApplication() {
        assertThatThrownBy(() -> resolver.resolveApplication(new MessageAccessContext(" ")))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void resolve_validatesExplicitEnvironmentAgainstSessionApplication() {
        MessageAccessContext context = new MessageAccessContext(APPLICATION_ID);
        ApplicationData application = new ApplicationData(UUID.fromString(APPLICATION_ID), "app");
        EnvironmentData environment = new EnvironmentData(UUID.fromString(ENVIRONMENT_ID),
                application, EnvironmentTypeData.build());
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(java.util.Optional.of(environment));

        String result = resolver.resolve(context, ENVIRONMENT_ID);

        assertThat(result).isEqualTo(ENVIRONMENT_ID);
        verify(environmentRepository, times(2)).findById(ENVIRONMENT_ID);
    }

    @Test
    void resolve_rejectsEnvironmentNotBelongingToApplication() {
        MessageAccessContext context = new MessageAccessContext(APPLICATION_ID);
        ApplicationData foreignApplication = ApplicationData.build();
        foreignApplication.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614175099"));
        EnvironmentData foreignEnvironment = new EnvironmentData(UUID.fromString(ENVIRONMENT_ID),
                foreignApplication, EnvironmentTypeData.build());
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(java.util.Optional.of(foreignEnvironment));
        when(catalogPort.getMessage("FUN_036")).thenReturn("Ambiente ajeno");

        assertThatThrownBy(() -> resolver.resolve(context, ENVIRONMENT_ID))
                .isInstanceOf(co.edu.uco.crosscutting.exceptions.ForbiddenException.class);

        verify(environmentRepository, times(2)).findById(ENVIRONMENT_ID);
    }
}
