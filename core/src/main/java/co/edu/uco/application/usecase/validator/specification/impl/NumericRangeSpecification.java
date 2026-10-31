package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilNumeric.isBetweenIncludingRanges;
import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

/**
 * Especificación que se cumple cuando un valor numérico está dentro de un rango inclusivo.
 */
public final class NumericRangeSpecification<T extends Number> implements Specification<T> {

    private final T minInclusive;
    private final T maxInclusive;

    public NumericRangeSpecification(T minInclusive, T maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !isNullObject(candidate) && isBetweenIncludingRanges(candidate, minInclusive, maxInclusive);
    }
}