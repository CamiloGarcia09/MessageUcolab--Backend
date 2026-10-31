package co.edu.uco.application.secondaryports.catalog;

import co.edu.uco.application.common.catalog.MessageCatalog;

public interface CatalogPort {

    MessageCatalog getMessageModel(String key);
    String getMessage(String key);
    String getMessage(String key, String defaultMessage);
    String getTitle(String key);
    void setMessage(String key, MessageCatalog message);
}