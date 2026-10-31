package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import java.util.List;

public interface MessageCategoryCatalogRepository {
    List<MessageCategoryData> findAll();
}
