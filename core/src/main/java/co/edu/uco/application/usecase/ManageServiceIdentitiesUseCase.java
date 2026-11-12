package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.ServiceIdentityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityClientIdValidator;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityEnvironmentCompositeValidator;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

import java.util.List;

import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_235;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_237;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * El propietario de una aplicacion asocia a cada ambiente las identidades de servicio que lo consumen.
 * Con esa relacion, MessageUcoLab sabe la aplicacion y el ambiente a partir del client_id del token.
 * La propiedad de la aplicacion la valida antes la sesion BFF.
 */
public final class ManageServiceIdentitiesUseCase {

    private final ServiceIdentityRepository repository;
    private final ServiceIdentityEnvironmentCompositeValidator environmentValidator;
    private final ServiceIdentityClientIdValidator clientIdValidator;
    private final CatalogPort catalog;

    public ManageServiceIdentitiesUseCase(final ServiceIdentityRepository repository,
                                          final ServiceIdentityEnvironmentCompositeValidator environmentValidator,
                                          final ServiceIdentityClientIdValidator clientIdValidator,
                                          final CatalogPort catalog) {
        this.repository = repository;
        this.environmentValidator = environmentValidator;
        this.clientIdValidator = clientIdValidator;
        this.catalog = catalog;
    }

    public List<ServiceIdentityDTO> list(final String applicationId) {
        return repository.findByApplicationId(applicationId).stream()
                .map(ManageServiceIdentitiesUseCase::toDto)
                .toList();
    }

    public void register(final String applicationId, final RegisterServiceIdentityDTO request) {
        String environmentId = request == null ? null : request.environmentId();
        environmentValidator.validate(applicationId, environmentId);
        String clientId = clientIdValidator.validate(request == null ? null : request.clientId());
        if (repository.findByClientId(clientId).isPresent()) {
            throw ConflictException.buildUserException(catalog.getMessage(FUN_235.getCode()));
        }
        repository.register(clientId, applicationId, getUUIDFromString(environmentId).toString());
    }

    public void remove(final String applicationId, final String clientId) {
        String validClientId = clientIdValidator.validate(clientId);
        if (!repository.remove(validClientId, applicationId)) {
            throw NotFoundException.buildUserException(catalog.getMessage(FUN_237.getCode()));
        }
    }

    private static ServiceIdentityDTO toDto(final ServiceIdentityData data) {
        return new ServiceIdentityDTO(data.clientId(), data.environmentId(), data.environmentName(), data.createdAt());
    }
}
