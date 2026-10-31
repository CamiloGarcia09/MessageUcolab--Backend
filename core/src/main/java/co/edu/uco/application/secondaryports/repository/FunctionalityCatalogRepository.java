package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import java.util.List;

public interface FunctionalityCatalogRepository {
    List<FunctionalityData> findAllByApplicationId(String applicationId);
}
