package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;

import java.util.List;
import java.util.Optional;

public interface ServiceIdentityRepository {

    Optional<ServiceIdentityData> findByClientId(String clientId);

    List<ServiceIdentityData> findByApplicationId(String applicationId);

    void register(String clientId, String applicationId, String environmentId);

    /** Devuelve false si la identidad no estaba registrada en la aplicacion. */
    boolean remove(String clientId, String applicationId);
}
