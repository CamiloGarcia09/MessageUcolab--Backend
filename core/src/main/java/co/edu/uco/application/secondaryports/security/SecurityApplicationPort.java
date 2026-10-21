package co.edu.uco.application.secondaryports.security;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;

import java.util.List;

/**
 * Consulta en el componente de seguridad las aplicaciones asignadas al usuario
 * de la sesion BFF. El valor de sesion nunca se persiste ni se registra.
 */
public interface SecurityApplicationPort {

    List<CatalogItemDTO> findCurrentUserApplications(String session);
}
