package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import java.util.List;

public interface MessageTypeCatalogRepository {
    List<MessageTypeData> findAll();
}
