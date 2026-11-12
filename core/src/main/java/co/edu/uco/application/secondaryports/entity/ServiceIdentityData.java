package co.edu.uco.application.secondaryports.entity;

/**
 * Identidad de servicio del componente de seguridad asociada a un ambiente de una aplicacion de MessageUcoLab.
 * El client_id es unico: cada identidad consume un solo ambiente.
 */
public record ServiceIdentityData(String clientId, String applicationId, String environmentId,
                                  String environmentName, String createdAt) {
}
