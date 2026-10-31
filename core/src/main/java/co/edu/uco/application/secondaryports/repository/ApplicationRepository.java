package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {

    Optional<ApplicationData> findByName(String name);

    boolean existsByNameAndOrganization(String name, String organizationId);

    Optional<ApplicationData> findById(String id);

    boolean existsById(String id);

    void createWithEnvironments(ApplicationData application, String languageId, String stateId,
                                List<EnvironmentData> environments, String environmentStateId);
}
