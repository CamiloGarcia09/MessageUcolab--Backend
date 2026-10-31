package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericRangeSpecificationTest {

    private final NumericRangeSpecification<Integer> specification = new NumericRangeSpecification<>(1, 10);

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsBelowRange() {
        assertFalse(specification.isSatisfiedBy(0));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsAboveRange() {
        assertFalse(specification.isSatisfiedBy(11));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsAtLowerBound() {
        assertTrue(specification.isSatisfiedBy(1));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsAtUpperBound() {
        assertTrue(specification.isSatisfiedBy(10));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsInsideRange() {
        assertTrue(specification.isSatisfiedBy(5));
    }
}