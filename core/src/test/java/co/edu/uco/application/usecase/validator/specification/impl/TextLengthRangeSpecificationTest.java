package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextLengthRangeSpecificationTest {

    private final TextLengthRangeSpecification specification = new TextLengthRangeSpecification(3, 5);

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsNull() {
        assertTrue(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsInsideRange() {
        assertTrue(specification.isSatisfiedBy("abcd"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsAtLowerBound() {
        assertTrue(specification.isSatisfiedBy("abc"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsAtUpperBound() {
        assertTrue(specification.isSatisfiedBy("abcde"));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenLengthIsBelowRange() {
        assertFalse(specification.isSatisfiedBy("ab"));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenLengthIsAboveRange() {
        assertFalse(specification.isSatisfiedBy("abcdef"));
    }

    @Test
    void isSatisfiedBy_trimsBeforeEvaluating() {
        assertTrue(specification.isSatisfiedBy("  abcde  "));
    }
}