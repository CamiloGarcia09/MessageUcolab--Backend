package co.edu.uco.application.secondaryports.repository;

/**
 * Catálogos de referencia a los que apuntan los enlaces de registro de la base de datos.
 * Cada constante identifica la tabla con la que se vinculan los id de los catálogos
 * Los identificadores de todos los registros catalogales son UUID persistentes.
 */
public enum ReferenceCatalog {

    APPLICATION_STATE("application_state"),
    ENVIRONMENT_STATE("environment_state"),
    ENVIRONMENT_TYPE("environment_type"),
    FUNCTIONALITY_STATE("functionality_state"),
    LANGUAGE_BASE("language_base"),
    MESSAGE_TYPE("message_type"),
    MESSAGE_CATEGORY("message_category"),
    MESSAGE_STATE("message_state"),
    MESSAGE_ENVIRONMENT_STATE("message_environment_state");

    private final String table;

    ReferenceCatalog(String table) {
        this.table = table;
    }

    public String getTable() {
        return table;
    }
}
