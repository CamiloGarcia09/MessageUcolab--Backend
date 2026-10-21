package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.session.SessionApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.security.SecurityApplicationPort;
import co.edu.uco.application.usecase.handling.HandlingSessionApplicationPort;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationRegistrationCompositeValidator;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.UnauthorizedException;

import java.util.List;
import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * Resuelve el alcance de negocio a partir de las aplicaciones que el componente de
 * seguridad asigna al usuario de la sesion. La aplicacion de negocio comparte el
 * UUID de la aplicacion de seguridad.
 */
public final class SessionApplicationUseCase implements HandlingSessionApplicationPort {

    private final SecurityApplicationPort securityApplicationPort;
    private final ApplicationRepository applicationRepository;
    private final SessionApplicationCompositeValidator validator;
    private final SessionApplicationAccessCompositeValidator accessValidator;
    private final SessionApplicationRegistrationCompositeValidator registrationValidator;
    private final CatalogPort catalogPort;

    public SessionApplicationUseCase(final SecurityApplicationPort securityApplicationPort,
                                     final ApplicationRepository applicationRepository,
                                     final SessionApplicationCompositeValidator validator,
                                     final SessionApplicationAccessCompositeValidator accessValidator,
                                     final SessionApplicationRegistrationCompositeValidator registrationValidator,
                                     final CatalogPort catalogPort) {
        this.securityApplicationPort = securityApplicationPort;
        this.applicationRepository = applicationRepository;
        this.validator = validator;
        this.accessValidator = accessValidator;
        this.registrationValidator = registrationValidator;
        this.catalogPort = catalogPort;
    }

    @Override
    public List<CatalogItemDTO> findCurrentUserApplications(final String session) {
        if (!new TextRequiredSpecification().isSatisfiedBy(session)) {
            throw UnauthorizedException.buildUserException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_152.getCode()));
        }
        return securityApplicationPort.findCurrentUserApplications(session);
    }

    @Override
    public List<SessionApplicationDTO> findSessionApplications(final List<CatalogItemDTO> sessionApplications) {
        if (sessionApplications == null) {
            return List.of();
        }
        return sessionApplications.stream()
                .filter(application -> new ValidUuidSpecification().isSatisfiedBy(application.getId()))
                .map(this::toSessionApplication)
                .toList();
    }

    @Override
    public String resolveApplication(final List<CatalogItemDTO> sessionApplications, final String applicationId) {
        accessValidator.validate(new SessionApplicationValidationContext(sessionApplications, applicationId, null));
        return getUUIDFromString(applicationId).toString();
    }

    // Ambiente explicito (por ejemplo para resolver un mensaje): debe pertenecer a la aplicacion.
    @Override
    public String resolveEnvironment(final List<CatalogItemDTO> sessionApplications,
                                     final String applicationId,
                                     final String environmentId) {
        validator.validate(new SessionApplicationValidationContext(sessionApplications, applicationId, environmentId));
        return getUUIDFromString(environmentId).toString();
    }

    @Override
    public void authorizeApplication(final List<CatalogItemDTO> sessionApplications, final String applicationId) {
        registrationValidator.validate(new SessionApplicationValidationContext(sessionApplications, applicationId, null));
    }

    private SessionApplicationDTO toSessionApplication(final CatalogItemDTO application) {
        var id = getUUIDFromString(application.getId()).toString();
        Optional<ApplicationData> registered = applicationRepository.findById(id);
        return SessionApplicationDTO.builder()
                .id(id)
                .name(application.getName())
                .registered(registered.isPresent())
                .organization(registered.map(this::organizationOf).orElse(null))
                .build();
    }

    private CatalogItemDTO organizationOf(final ApplicationData application) {
        var organization = application.getOrganization();
        return organization == null || organization.getId() == null
                ? null
                : CatalogItemDTO.create(organization.getId().toString(), organization.getName());
    }
}
