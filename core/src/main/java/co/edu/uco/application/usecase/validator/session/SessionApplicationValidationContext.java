package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;

import java.util.List;

public record SessionApplicationValidationContext(List<CatalogItemDTO> sessionApplications,
                                                  String applicationId,
                                                  String environmentId) {
}
