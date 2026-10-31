package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import java.util.List;

public interface MessageEnvironmentStateCatalogRepository {
    List<MessageEnvironmentStateData> findAll();
}
