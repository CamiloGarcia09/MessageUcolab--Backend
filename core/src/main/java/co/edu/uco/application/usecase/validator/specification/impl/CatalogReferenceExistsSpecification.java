package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.specification.Specification;

/**
 * Especificación que se cumple cuando un id de catálogo de referencia existe en la base de datos.
 */
public final class CatalogReferenceExistsSpecification implements Specification<String> {

    private final RecordExistsCatalogPort recordExistsCatalogPort;
    private final ReferenceCatalog catalog;

    public CatalogReferenceExistsSpecification(RecordExistsCatalogPort recordExistsCatalogPort, ReferenceCatalog catalog) {
        this.recordExistsCatalogPort = recordExistsCatalogPort;
        this.catalog = catalog;
    }

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return new ValidUuidSpecification().isSatisfiedBy(candidate)
                && recordExistsCatalogPort.exists(catalog, candidate);
    }
}
