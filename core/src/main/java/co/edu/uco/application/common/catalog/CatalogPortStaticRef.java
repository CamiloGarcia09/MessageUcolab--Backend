package co.edu.uco.application.common.catalog;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

/**
 * Static holder so non-Spring code (domain exceptions, serializers) can resolve
 * catalog messages without requiring Spring injection.
 * Initialized with a bootstrap catalog so startup failures are always reportable.
 * Overridden by RedisCatalogMessageAdapter on @PostConstruct.
 */
public final class CatalogPortStaticRef {

    private static volatile CatalogPort instance = new BootstrapMessageCatalogPort();

    private CatalogPortStaticRef() {}

    public static void set(CatalogPort catalog) {
        instance = isNullObject(catalog) ? new BootstrapMessageCatalogPort() : catalog;
    }

    public static String getMessage(String key) {
        return instance().getMessage(key);
    }

    public static String getMessage(String key, String defaultMessage) {
        return instance().getMessage(key, defaultMessage);
    }

    public static String getTitle(String key) {
        return instance().getTitle(key);
    }

    public static MessageCatalog getMessageModel(String key) {
        return instance().getMessageModel(key);
    }

    private static CatalogPort instance() {
        return instance;
    }
}