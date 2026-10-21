package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.security.SecurityApplicationPort;
import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationRegistrationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationUseCaseTest {

    private static final String SESSION = "session-value";
    private static final String REGISTERED_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String UNREGISTERED_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final UUID ORGANIZATION_ID = UUID.fromString("8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40");

    @Mock
    private SecurityApplicationPort securityApplicationPort;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private SessionApplicationCompositeValidator validator;
    @Mock
    private SessionApplicationAccessCompositeValidator accessValidator;
    @Mock
    private SessionApplicationRegistrationCompositeValidator registrationValidator;
    @Mock
    private CatalogPort catalogPort;

    private SessionApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SessionApplicationUseCase(securityApplicationPort, applicationRepository, validator,
                accessValidator, registrationValidator, catalogPort);
    }

    @Test
    void resolveApplication_validatesSessionAccessAndReturnsNormalizedId() {
        List<CatalogItemDTO> session = List.of(CatalogItemDTO.create(REGISTERED_ID, "MessageUcoLab"));

        String applicationId = useCase.resolveApplication(session, REGISTERED_ID.toUpperCase());

        assertThat(applicationId).isEqualTo(REGISTERED_ID);
        verify(accessValidator).validate(
                new SessionApplicationValidationContext(session, REGISTERED_ID.toUpperCase(), null));
        verifyNoInteractions(validator, registrationValidator);
    }

    @Test
    void resolveApplication_propagatesAccessError() {
        doThrow(ForbiddenException.buildUserException("Permission denied")).when(accessValidator).validate(any());

        assertThatThrownBy(() -> useCase.resolveApplication(List.of(), UNREGISTERED_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findCurrentUserApplications_delegatesSessionToSecurityPort() {
        List<CatalogItemDTO> applications = List.of(CatalogItemDTO.create(REGISTERED_ID, "MessageUcoLab"));
        when(securityApplicationPort.findCurrentUserApplications(SESSION)).thenReturn(applications);

        assertThat(useCase.findCurrentUserApplications(SESSION)).isSameAs(applications);
    }

    @ParameterizedTest(name = "session=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void findCurrentUserApplications_throwsUnauthorizedWithoutCallingPort_whenSessionIsMissing(String session) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode())).thenReturn("Authentication required");

        assertThatThrownBy(() -> useCase.findCurrentUserApplications(session))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(exception -> assertThat(((UnauthorizedException) exception).getUserMessage())
                        .isEqualTo("Authentication required"));

        verifyNoInteractions(securityApplicationPort);
    }

    @Test
    void findSessionApplications_marksRegisteredApplicationsAndIncludesTheirOrganization() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(ORGANIZATION_ID);
        organization.setName("UCO");
        when(applicationRepository.findById(REGISTERED_ID)).thenReturn(Optional.of(
                new ApplicationData(UUID.fromString(REGISTERED_ID), "MessageUcoLab", organization)));
        when(applicationRepository.findById(UNREGISTERED_ID)).thenReturn(Optional.empty());

        List<SessionApplicationDTO> result = useCase.findSessionApplications(List.of(
                CatalogItemDTO.create(REGISTERED_ID, "MessageUcoLab"),
                CatalogItemDTO.create(UNREGISTERED_ID, "carga masiva")));

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .extracting(SessionApplicationDTO::getId, SessionApplicationDTO::getName, SessionApplicationDTO::isRegistered)
                .containsExactly(REGISTERED_ID, "MessageUcoLab", true);
        assertThat(result.get(0).getOrganization()).isEqualTo(CatalogItemDTO.create(ORGANIZATION_ID.toString(), "UCO"));
        assertThat(result.get(1))
                .extracting(SessionApplicationDTO::getId, SessionApplicationDTO::getName, SessionApplicationDTO::isRegistered)
                .containsExactly(UNREGISTERED_ID, "carga masiva", false);
        assertThat(result.get(1).getOrganization()).isNull();
    }

    @Test
    void findSessionApplications_normalizesIdsAndSkipsApplicationsWithoutUsableId() {
        when(applicationRepository.findById(REGISTERED_ID)).thenReturn(Optional.empty());

        List<SessionApplicationDTO> result = useCase.findSessionApplications(List.of(
                CatalogItemDTO.create(REGISTERED_ID.toUpperCase(), "MessageUcoLab"),
                CatalogItemDTO.create("", "Sin id"),
                CatalogItemDTO.create("not-a-uuid", "Invalida")));

        assertThat(result).extracting(SessionApplicationDTO::getId).containsExactly(REGISTERED_ID);
    }

    @Test
    void findSessionApplications_returnsEmpty_whenSessionApplicationsAreNull() {
        assertThat(useCase.findSessionApplications(null)).isEmpty();

        verifyNoInteractions(applicationRepository);
    }

    @Test
    void resolveEnvironment_validatesContextAndReturnsNormalizedEnvironmentId() {
        List<CatalogItemDTO> session = List.of(CatalogItemDTO.create(REGISTERED_ID, "MessageUcoLab"));
        ArgumentCaptor<SessionApplicationValidationContext> captor =
                ArgumentCaptor.forClass(SessionApplicationValidationContext.class);

        String environmentId = useCase.resolveEnvironment(session, REGISTERED_ID, ENVIRONMENT_ID.toUpperCase());

        assertThat(environmentId).isEqualTo(ENVIRONMENT_ID);
        verify(validator).validate(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new SessionApplicationValidationContext(session, REGISTERED_ID, ENVIRONMENT_ID.toUpperCase()));
    }

    @Test
    void resolveEnvironment_propagatesValidationError() {
        doThrow(ForbiddenException.buildUserException("Permission denied")).when(validator).validate(any());

        assertThatThrownBy(() -> useCase.resolveEnvironment(List.of(), UNREGISTERED_ID, ENVIRONMENT_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorizeApplication_validatesWithRegistrationValidatorAndNoEnvironment() {
        List<CatalogItemDTO> session = List.of(CatalogItemDTO.create(REGISTERED_ID, "MessageUcoLab"));

        useCase.authorizeApplication(session, REGISTERED_ID);

        verify(registrationValidator).validate(new SessionApplicationValidationContext(session, REGISTERED_ID, null));
        verifyNoInteractions(validator);
    }
}
