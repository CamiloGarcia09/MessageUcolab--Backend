package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isActive;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;

public final class ActiveFunctionalitySpecification implements Specification<FunctionalityData> {

    @Override
    public boolean isSatisfiedBy(FunctionalityData candidate) {
        return candidate != null
                && !DEFAULT_UUID.equals(candidate.getStateId())
                && isActive(candidate.getState());
    }
}
