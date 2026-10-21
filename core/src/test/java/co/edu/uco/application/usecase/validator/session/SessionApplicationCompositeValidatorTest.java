package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationCompositeValidatorTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_APPLICATION_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final List<CatalogItemDTO> SESSION = List.of(CatalogItemDTO.create(APPLICATION_ID, "MessageUcoLab"));

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private EnvironmentRepository environmentRepository;

    private SessionApplicationCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SessionApplicationCompositeValidator(catalogPort, applicationRepository, environmentRepository);
    }

    @Test
    void validate_acceptsAssignedRegisteredApplicationAndOwnEnvironment() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application()));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentOf(APPLICATION_ID)));

        assertThatCode(() -> validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID,
                ENVIRONMENT_ID))).doesNotThrowAnyException();
    }

    @Test
    void validate_checksApplicationBeforeEnvironment() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application()));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentOf(APPLICATION_ID)));

        validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID, ENVIRONMENT_ID));

        // Existencia y pertenencia del ambiente consultan el repositorio por separado.
        InOrder order = inOrder(applicationRepository, environmentRepository);
        order.verify(applicationRepository).findById(APPLICATION_ID);
        order.verify(environmentRepository, times(2)).findById(ENVIRONMENT_ID);
    }

    @Test
    void validate_shortCircuitsBeforeRepositories_whenIdentifiersAreMissing() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_155.getCode())).thenReturn("Identifiers required");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID, "")))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(applicationRepository, environmentRepository);
    }

    @Test
    void validate_shortCircuitsBeforeRepositories_whenIdentifierIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> validator.validate(
                new SessionApplicationValidationContext(SESSION, APPLICATION_ID, "not-a-uuid")))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(applicationRepository, environmentRepository);
    }

    @Test
    void validate_rejectsWithForbiddenBeforeRepositories_whenApplicationIsNotInSession() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn("Permission denied");

        assertThatThrownBy(() -> validator.validate(
                new SessionApplicationValidationContext(SESSION, OTHER_APPLICATION_ID, ENVIRONMENT_ID)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(applicationRepository, environmentRepository);
    }

    @Test
    void validate_rejectsWithNotFound_whenAssignedApplicationIsNotRegistered() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_157.getCode())).thenReturn("Application not found");

        assertThatThrownBy(() -> validator.validate(
                new SessionApplicationValidationContext(SESSION, APPLICATION_ID, ENVIRONMENT_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(environmentRepository, never()).findById(ENVIRONMENT_ID);
    }

    @Test
    void validate_rejectsWithConflict_whenEnvironmentBelongsToAnotherApplication() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application()));
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentOf(OTHER_APPLICATION_ID)));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn("Conflict");

        assertThatThrownBy(() -> validator.validate(
                new SessionApplicationValidationContext(SESSION, APPLICATION_ID, ENVIRONMENT_ID)))
                .isInstanceOf(ConflictException.class);
    }

    private static ApplicationData application() {
        return ApplicationData.build(UUID.fromString(APPLICATION_ID), "MessageUcoLab");
    }

    private static EnvironmentData environmentOf(String applicationId) {
        return new EnvironmentData(UUID.fromString(ENVIRONMENT_ID),
                ApplicationData.build(UUID.fromString(applicationId), "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"));
    }
}
