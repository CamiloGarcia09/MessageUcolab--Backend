package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.ApplicationData;
import java.util.List;

public interface ApplicationCatalogRepository {
    List<ApplicationData> findAll();
}
