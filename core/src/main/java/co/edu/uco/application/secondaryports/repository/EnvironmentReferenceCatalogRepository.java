package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentReferenceCatalogRepository {

    List<EnvironmentTypeData> findAllTypes();

    Optional<UUID> findStateIdByName(String name);
}
