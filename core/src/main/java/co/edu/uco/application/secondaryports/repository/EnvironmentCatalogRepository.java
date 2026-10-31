package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import java.util.List;

public interface EnvironmentCatalogRepository {
    List<EnvironmentData> findAllByApplicationId(String applicationId);
}
