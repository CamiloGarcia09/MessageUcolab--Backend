package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import java.util.List;

public interface MessageStateCatalogRepository {
    List<StatusMessageData> findAll();
}
