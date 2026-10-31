package co.edu.uco.application.secondaryports.repository;

/**
 * Puerta de salida para verificar que un registro de catálogo (estado, tipo o idioma)
 * referenciado por un enlace de registro existe en la base de datos.
 */
public interface RecordExistsCatalogPort {

    boolean exists(ReferenceCatalog reference, String id);
}