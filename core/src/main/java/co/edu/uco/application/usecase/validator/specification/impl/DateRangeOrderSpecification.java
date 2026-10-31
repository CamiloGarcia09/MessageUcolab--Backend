package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

/**
 * Especificación que se cumple cuando la fecha de inicio no es posterior a la fecha de fin.
 */
public final class DateRangeOrderSpecification implements Specification<DateTimeRange> {

    @Override
    public boolean isSatisfiedBy(DateTimeRange candidate) {
        return candidate != null && !candidate.startIsAfterEnd();
    }
}